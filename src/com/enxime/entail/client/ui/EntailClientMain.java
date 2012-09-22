package com.enxime.entail.client.ui;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.enxime.entail.share.LogUtil;

public class EntailClientMain {
	private static final Logger _logger = LogUtil
			.getLogger(EntailClientMain.class.getName());

	// CTabItemWrapperManager cTabItemWrapperManager = new
	// CTabItemWrapperManager();
	// private AbstractTailServerManager clientWrapper;
	private Shell shell;
	private Label urlLabel;
	private Text urlText;
	private Button startButton;
	private Button pauseButton;
	private CTabFolder folder;
	private MessageBox messageBox;

	// private HashMap<String, CTabItemWrapper> cTabItemWrapperMap = new
	// HashMap<String, CTabItemWrapper>();

	private void draw() {
		// this.clientWrapper = new AbstractTailServerManager(this);

		Display display = new Display();

		final Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(4, false));
		this.shell = shell;

		Label urlLabel = new Label(shell, SWT.NONE);
		urlLabel.setText("URL:");
		this.urlLabel = urlLabel;

		// Text for Tail URL
		final Text urlText = new Text(shell, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		urlText.setLayoutData(gridData);
		urlText.setText("tail://");
		urlText.setFocus();

		urlText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent modifyEvent) {
				// TODO Auto-generated method stub
				// _logger.fine("URL text modified: " + urlText.getText());
				// if(!startButton.isEnabled())
				// startButton.setEnabled(true);
				// if(pauseButton.isEnabled())
				// pauseButton.setEnabled(false);
			}

		});

		this.urlText = urlText;

		// Button to start tailing
		final Button startButton = new Button(shell, SWT.PUSH);
		startButton.setText(TailConstant.START);
		startButton.addSelectionListener(new StartButtonSelectionAdapter());
		this.startButton = startButton;

		// Button to pause tailing
		final Button pauseButton = new Button(shell, SWT.PUSH);
		pauseButton.setText(TailConstant.PAUSE);
		pauseButton.addSelectionListener(new PauseButtonSelectionAdapter());
		pauseButton.setEnabled(false);
		this.pauseButton = pauseButton;

		final CTabFolder folder = new CTabFolder(shell, SWT.BORDER);
		GridData folderGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		folderGridData.horizontalSpan = 4;
		folder.setLayoutData(folderGridData);
		folder.setSimple(false);
		folder.setUnselectedImageVisible(false);
		folder.setUnselectedCloseVisible(false);

		folder.setMinimizeVisible(false);
		folder.setMaximizeVisible(false);

		folder.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				_logger.fine("Selected CTabItem: "
						+ folder.getSelection().getToolTipText());

				CTabItemWrapper cTabItemWrapper = CTabItemWrapperManager
						.getInstance()
						.getCTabItemWrapper(folder.getSelection());
				urlText.setText(cTabItemWrapper.getTailUrl().toString());
				// startButton.setEnabled(false);
				TailState tailState = cTabItemWrapper.getTailState();
				if (tailState == TailState.RUNNING) {
					pauseButton.setText("Pause");
				} else if (tailState == TailState.PAUSED) {
					pauseButton.setText("Restart");
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent selectionEvent) {
				widgetSelected(selectionEvent);
			}
		});

		this.folder = folder;

		shell.setSize(640, 480);
		// shell.pack();
		shell.open();

		this.messageBox = new MessageBox(shell);
		// Point point = display.getCursorLocation();
		// this.messageBox.setLocation(pt.x, pt.y);
		// this.messageBox.getParent().setLocation(point);

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		display.dispose();

	}

	public void showAlert(String message) {
		Display.getDefault().asyncExec(new AlertThread(message));
	}

	class AlertThread extends Thread {
		String message;

		// EntailClientMain entailClientMain;

		AlertThread(/* EntailClientMain entailClientMain, */String message) {
			// this.entailClientMain = entailClientMain;
			this.message = message;
		}

		public void run() {
			_logger.fine("called.");
			if (!messageBox.getParent().isDisposed()) {
				messageBox.setText("Alert");
				messageBox.setMessage(this.message);
				messageBox.open();
			}
		}
	}

	class StartButtonSelectionAdapter extends SelectionAdapter {
		// EntailClientMain entailClientMain;

		public StartButtonSelectionAdapter(/* EntailClientMain entailClientMain */) {
			// this.entailClientMain = entailClientMain;
		}

		public void widgetSelected(SelectionEvent event) {
			// this.entailClientMain.clientWrapper.add(this.entailClientMain.folder,
			// this.entailClientMain.urlText.getText());
			_logger.fine(event.text);
			try {
				TailURL tailUrl = new TailURL(urlText.getText());
				if (CTabItemWrapperManager.getInstance().hasAlready(tailUrl)) {
					showAlert(tailUrl.toString() + " is already tailed.");
				}
				boolean success = CTabItemWrapperManager.getInstance()
						.addCTabItemWrapper(folder, tailUrl);
				if (success) {
					success = TailClientControllerManager.getInstance()
							.addTailURL(tailUrl);
				}
				if (success) {

					// startButton.setEnabled(false);
					pauseButton.setEnabled(true);
				}
			} catch (InvalidTailURLException e) {
				// TODO Auto-generated catch block
				// entailClientMain.showAlert(e.getMessage());
				showAlert(e.getMessage());
			}
		}
	}

	class PauseButtonSelectionAdapter extends SelectionAdapter {
		// EntailClientMain entailClientMain;

		public PauseButtonSelectionAdapter() {
		}

		public void widgetSelected(SelectionEvent event) {

			_logger.fine("called.");
			CTabItemWrapper cTabItemWrapper = CTabItemWrapperManager
					.getInstance().getCTabItemWrapper(folder.getSelection());
			if (cTabItemWrapper != null) {
				TailState tailState = cTabItemWrapper.getTailState();
				if (tailState == TailState.RUNNING) {
					TailClientControllerManager.getInstance().suspendTailURL(
							cTabItemWrapper.getTailUrl());
					cTabItemWrapper.setTailState(TailState.PAUSED);
					pauseButton.setText(TailConstant.RESTART);

				} else if (tailState == TailState.PAUSED) {
					TailClientControllerManager.getInstance().restartTailURL(
							cTabItemWrapper.getTailUrl());
					cTabItemWrapper.setTailState(TailState.RUNNING);
					pauseButton.setText(TailConstant.PAUSE);

				}

			}

		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EntailClientMain mainWindow = new EntailClientMain();
		mainWindow.draw();
	}

}
