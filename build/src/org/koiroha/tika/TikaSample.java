/* **************************************************************************
 * Copyright (C) 2011 BJoRFUAN. All Rights Reserved
 * **************************************************************************
 * This module, contains source code, binary and documentation, is in the
 * Apache License Ver. 2.0, and comes with NO WARRANTY.
 *
 *                                           takami torao <koiroha@gmail.com>
 *                                                   http://www.bjorfuan.com/
 */
package org.koiroha.tika;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.prefs.*;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import org.apache.tika.Tika;

// ==========================================================================
// TikaSample: Tika 実行サンプル
// ==========================================================================
/**
 * Apache Tika の動作確認用サンプルです。選択したファイルの内容からテキストを
 * 表示します。
 *
 * @author takami torao
 */
public class TikaSample extends JFrame {

	// ======================================================================
	// シリアルバージョン
	// ======================================================================
	/**
	 * このクラスのシリアルバージョンです。
	 */
	private static final long serialVersionUID = 1L;

	// ======================================================================
	// ファイル名
	// ======================================================================
	/**
	 * ファイル名の表示領域です。
	 */
	private final JLabel fileName = new JLabel();

	// ======================================================================
	// MIME-Type
	// ======================================================================
	/**
	 * ファイル内容の MIME-Type です。
	 */
	private final JLabel mimeType = new JLabel();

	// ======================================================================
	// テキスト内容表示領域
	// ======================================================================
	/**
	 * Tika で取得したテキストの内容を表示する領域です。
	 */
	private final JTextArea plain = new JTextArea();

	// ======================================================================
	// コンストラクタ
	// ======================================================================
	/**
	 * コンストラクタは何も行いません。
	 */
	public TikaSample() {
		this.setTitle("Apache Tika Document Sampling Toolkit");

		// コンポーネントの配置
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(new GridBagLayout());

		// ファイル名ラベル
		fileName.setFont(fileName.getFont().deriveFont(18f));
		fileName.setText("ファイルをドロップしてください");
		c.gridx = 0;		c.gridy = 0;
		c.gridwidth = 1;	c.gridheight = 1;
		c.weightx = 1.0;	c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add(fileName, c);

		// Tika 解析結果
		plain.setEditable(false);
		plain.setLineWrap(true);
		c.gridx = 0;		c.gridy = 1;
		c.gridwidth = 1;	c.gridheight = 1;
		c.weightx = 1.0;	c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
		this.add(new JScrollPane(plain), c);

		// MIME-Type ラベル
		mimeType.setFont(mimeType.getFont().deriveFont(Font.PLAIN));
		mimeType.setText(" ");
		mimeType.setBorder(BorderFactory.createLoweredBevelBorder());
		c.gridx = 0;		c.gridy = 2;
		c.gridwidth = 1;	c.gridheight = 1;
		c.weightx = 1.0;	c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add(mimeType, c);

		// イベントハンドラの設定
		TransferHandler h = new TransferHandler(){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean canImport(TransferSupport support) {
				return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
			}
			@Override
			public boolean importData(TransferSupport support) {
				if(! canImport(support)){
					return false;
				}
				Transferable t = support.getTransferable();
				try {
					@SuppressWarnings("unchecked")
					java.util.List<File> list = (java.util.List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);
					open(list);
				} catch(IOException ex){
					alert("ファイルのオープンに失敗しました", ex);
				} catch(UnsupportedFlavorException ex){
					alert("この環境は Drag & Drop がサポートされていません", ex);
				}
				return true;
			}

			/** クリップボードへのテキスト転送処理を追加。 */
			@Override
			public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
				if(comp instanceof JTextComponent){
					JTextComponent t = (JTextComponent)comp;
					String text = t.getSelectedText();
					Transferable tf = new StringSelection(text);
					clip.setContents(tf, null);
				}
				return;
			}
		};
		fileName.setTransferHandler(h);
		mimeType.setTransferHandler(h);
		plain.setTransferHandler(h);

		// 標準状態に設定
		this.pack();

		// ウィンドウ状態の復元
		Preferences pref = Preferences.userNodeForPackage(TikaSample.class);
		Rectangle area = getBounds();
		this.setBounds(
			pref.getInt("window.x", area.x),
			pref.getInt("window.y", area.y),
			pref.getInt("window.width", area.width),
			pref.getInt("window.height", area.height));
		int state = pref.getInt("window.state", NORMAL) & ~ICONIFIED;
		this.setExtendedState(state);

		// ウィンドウクローズ時の処理を追加
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});
		return;
	}

	// ======================================================================
	// ファイルのオープン
	// ======================================================================
	/**
	 * 指定されたファイルをオープンします。
	 *
	 * @param list オープンするファイルのリスト
	 */
	private void open(java.util.List<File> list) {

		Tika tika = new Tika();
		tika.setMaxStringLength(4000);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		for(File f: list){
			fileName.setText(f.getName());
			try {
				long start = System.currentTimeMillis();
				String text = tika.parseToString(f);
				long end = System.currentTimeMillis();
				pw.println(text);
				mimeType.setText(tika.detect(f) + " (" + NumberFormat.getNumberInstance().format(end-start) + "ms)");
			} catch(Exception ex){
				ex.printStackTrace(pw);
			}
			pw.flush();
		}

		// テキストの設定
		plain.setText(sw.toString());
		plain.setCaretPosition(0);
		return;
	}

	// ======================================================================
	// アプリケーションの終了
	// ======================================================================
	/**
	 * アプリケーションを終了します。
	 */
	private void exit(){

		// ウィンドウ状態の保存 (最小化状態は除外/最大化時は)
		int state = getExtendedState();
		Preferences pref = Preferences.userNodeForPackage(TikaSample.class);
		pref.putInt("window.state", state);

		// ウィンドウ領域の保存 (最大化時はもとに戻す)
		if((state & MAXIMIZED_BOTH) != 0){
			setExtendedState(state & ~MAXIMIZED_BOTH);
		}
		Rectangle area = getBounds();
		pref.putInt("window.x", area.x);
		pref.putInt("window.y", area.y);
		pref.putInt("window.width", area.width);
		pref.putInt("window.height", area.height);

		// 設定内容の保存
		try {
			pref.flush();
		} catch(BackingStoreException ex){
			alert("設定の保存に失敗しました", ex);
		}
		return;
	}

	// ======================================================================
	// エラーの通知
	// ======================================================================
	/**
	 * ユーザにエラーを通知します。
	 *
	 * @param msg エラーメッセージ
	 * @param ex 例外
	 */
	private void alert(String msg, Throwable ex){
		JOptionPane.showMessageDialog(this, msg, "ALERT", JOptionPane.ERROR_MESSAGE);
		return;
	}

	// ======================================================================
	// アプリケーションの実行
	// ======================================================================
	/**
	 * このアプリケーションを実行します。
	 *
	 * @param args コマンドライン引数
	 * @throws Exception 処理に失敗した場合
	*/
	public static void main(String[] args) throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run(){
				JFrame f = new TikaSample();
				f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				f.setVisible(true);
				return;
			}
		});
		return;
	}

}
