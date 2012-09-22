package com.enxime.entail.experiment;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.enxime.entail.client.ui.CTabItemWrapperManager;
import com.enxime.entail.client.ui.TailClientControllerManager;
import com.enxime.entail.client.ui.TailState;
import com.enxime.entail.client.ui.TailURL;
import com.enxime.entail.share.LogUtil;

public class CTabItemWrapperActive {
	private static final Logger _logger = LogUtil
			.getLogger(CTabItemWrapperActive.class.getName());

	private TailURL tailUrl;

	private TailState tailState = TailState.RUNNING;

	private CTabFolder cTabFolder;

	private CTabItem cTabItem;
	private CTabItemDisposeListener cTabItemDisposeListener;
	private Text tabItemBody;
	private Text tabItemMessage;
	private Composite composite;

	public CTabItemWrapperActive(CTabFolder cTabFolder, TailURL tailUrl) {
		_logger.fine("called.");
		this.tailUrl = tailUrl;
		this.cTabFolder = cTabFolder;
		this.cTabItem = new CTabItem(this.cTabFolder, SWT.CLOSE);
		this.cTabItemDisposeListener = new CTabItemDisposeListener(/* this */);
		this.cTabItem.addDisposeListener(this.cTabItemDisposeListener);

		this.cTabItem.setText(this.tailUrl.getFileName());
		this.cTabItem.setToolTipText(this.tailUrl.toString());
		this.composite = new Composite(this.cTabFolder, SWT.BORDER);
		GridLayout gridLayout4Composite = new GridLayout();
		gridLayout4Composite.numColumns = 1;
		this.composite.setLayout(gridLayout4Composite);
		this.tabItemBody = new Text(this.composite, SWT.MULTI | SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.READ_ONLY | SWT.BORDER);
		GridData gridData4TabItemBody = new GridData(SWT.FILL, SWT.FILL, true,
				true);
		this.tabItemBody.setLayoutData(gridData4TabItemBody);

		this.tabItemMessage = new Text(this.composite, SWT.SINGLE
				| SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.BORDER
				| SWT.WRAP);
		GridData gridData4TabItemMessage = new GridData();
		gridData4TabItemMessage.horizontalAlignment = GridData.FILL;
		gridData4TabItemMessage.verticalAlignment = GridData.VERTICAL_ALIGN_END;
		gridData4TabItemMessage.grabExcessHorizontalSpace = true;
		this.tabItemMessage.setLayoutData(gridData4TabItemMessage);
		this.cTabItem.setControl(this.composite);

		this.cTabFolder.getShell().layout(true);

		// this.setSelection();
	}

	public void setTailState(TailState tailState) {
		this.tailState = tailState;
	}

	public TailState getTailState() {
		return this.tailState;
	}

	public CTabItem getCTabItem() {
		return this.cTabItem;
	}

	public void appendText(List<String> textList) {
		for (String text : textList)
			this.tabItemBody.append(text);
	}

	public void appendTextFromOtherThread(List<String> textList) {
		this.cTabFolder.getDisplay().asyncExec(
				new AppendTextThread(/* this, */textList));
	}

	public void setMessage(String message) {
		this.tabItemMessage.setText(message);
	}

//	public void setMessageFromOtherThread(String message) {
//		// this.tabItemMessage.setText(message);
//		this.cTabFolder.getDisplay().asyncExec(
//				new SetMessageThread(/* this, */message));
//	}

	public void setSelection() {
		this.cTabFolder.setSelection(this.cTabItem);
	}

	public TailURL getTailUrl() {
		return this.tailUrl;
	}

	// public CTabItem getCTabItem() {
	// return this.cTabItem;
	// }

	public void disposeCTabItem() {
		_logger.fine("called.");

		this.cTabItem.dispose();
	}

	class AppendTextThread extends Thread {
		// CTabItemWrapper cTabItemWrapper;
		List<String> textList;

		AppendTextThread(
				/* CTabItemWrapper cTabItemWrapper, */List<String> textList) {
			// this.cTabItemWrapper = cTabItemWrapper;
			this.textList = textList;
		}

		public void run() {
			for (String text : this.textList)
				tabItemBody.append(text + "\n");
		}
	}

//	class SetMessageThread extends Thread {
//		// CTabItemWrapper tailClientController;
//		String message;
//
//		SetMessageThread(
//				/* CTabItemWrapper tailClientController, */String message) {
//			// this.tailClientController = tailClientController;
//			this.message = message;
//		}
//
//		public void run() {
//			if (this.message != null)
//				tabItemMessage.setText(this.message);
//		}
//	}

	class CTabItemDisposeListener implements DisposeListener {

		// private CTabItemWrapper cTabItemWrapper;

		public CTabItemDisposeListener(/* CTabItemWrapper cTabItemWrapper */) {
			_logger.fine("called.");

			// this.cTabItemWrapper = cTabItemWrapper;
		}

		@Override
		public void widgetDisposed(DisposeEvent arg0) {
			_logger.fine("called.");
			TailClientControllerManager.getInstance().removeTailURL(tailUrl);
			CTabItemWrapperManager.getInstance().removeCTabItemWrapper(tailUrl);
			// TODO something
		}
	}
}
