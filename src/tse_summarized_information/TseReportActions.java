package tse_summarized_information;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXException;

import app_config.AppPaths;
import app_config.PropertiesReader;
import global_utils.Message;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import message.SendMessageException;
import report.Report;
import report.ReportActions;
import report.ReportException;
import report.ReportSendOperation;
import report.UnsupportedReportActionException;
import soap.MySOAPException;
import tse_config.TSEWarnings;

public class TseReportActions extends ReportActions {

	private Shell shell;
	
	public TseReportActions(Shell shell, Report report) {
		super(shell, report);
		this.shell = shell;
	}

	@Override
	public void end(ReportAction action) {
		
		switch(action) {
		
		case SEND:
			sendEnd();
			break;
		case REJECT:
			rejectEnd();
			break;
		case SUBMIT:
			submitEnd();
			break;
		default:
			unsupportedEnd();
			break;
		}
	}
	
	@Override
	public void manageException(Exception e, ReportAction action) {
		
		switch(action) {
		case SEND:
			sendException(e);
			break;
		case REJECT:
		case SUBMIT:
			rejectSubmitException(e);
			break;
		default:
			break;
		}
	}
	
	private void rejectSubmitException(Exception e) {
		
		Message msg = null;
		
		if (e instanceof IOException || e instanceof ParserConfigurationException
				|| e instanceof SAXException) {
			msg = Warnings.createFatal(TSEMessages.get("report.io.error", PropertiesReader.getSupportEmail()));
		}
		else if (e instanceof SendMessageException) {
			msg = TSEWarnings.getSendMessageWarning((SendMessageException) e);
		}
		else if (e instanceof MySOAPException) {
			msg = Warnings.createSOAPWarning((MySOAPException) e);
		}
		else if (e instanceof ReportException) {
			String title = TSEMessages.get("error.title");
			String message = TSEMessages.get("report.unsupported.action");
			msg = Warnings.create(title, message, SWT.ICON_ERROR);
		}
		else {
			msg = Warnings.createFatal(TSEMessages.get("generic.error", PropertiesReader.getSupportEmail()));
		}
		
		if (msg != null)
			msg.open(shell);
	}
	
	private void sendException(Exception e) {
		
		Message msg = null;
		
		if (e instanceof IOException) {
			msg = Warnings.createFatal(TSEMessages.get("report.io.error", PropertiesReader.getSupportEmail()));
		}
		else if (e instanceof MySOAPException) {
			msg = Warnings.createSOAPWarning((MySOAPException) e);
		}
		else if (e instanceof SAXException || e instanceof ParserConfigurationException) {
			msg = Warnings.createFatal(TSEMessages.get("gde2.missing", AppPaths.MESSAGE_GDE2_XSD, 
					PropertiesReader.getSupportEmail()));
		}
		else if (e instanceof SendMessageException) {
			
			SendMessageException sendE = (SendMessageException) e;
			
			msg = TSEWarnings.getSendMessageWarning(sendE);
		}
		else if (e instanceof ReportException) {
			msg = Warnings.createFatal(TSEMessages.get("send.failed.no.senderId", PropertiesReader.getSupportEmail()));
		}
		else if (e instanceof UnsupportedReportActionException) {
			msg = getUnsupportedOpWarning(((UnsupportedReportActionException) e).getOperation());
		}
		else {
			msg = Warnings.createFatal(TSEMessages.get("generic.error", PropertiesReader.getSupportEmail()));
		}
		
		if (msg != null)
			msg.open(shell);
	}
	
	private void unsupportedEnd() {
		String title = TSEMessages.get("error.title");
		String message = TSEMessages.get("report.unsupported.action");
		Warnings.warnUser(shell, title, message);
	}
	
	private void sendEnd() {
		String title = TSEMessages.get("success.title");
		String message = TSEMessages.get("send.success");
		int icon = SWT.ICON_INFORMATION;
		
		Warnings.warnUser(shell, title, message, icon);
	}
	
	private void rejectEnd() {
		String title = TSEMessages.get("success.title");
		String message = TSEMessages.get("reject.success");
		int style = SWT.ICON_INFORMATION;
		
		Warnings.warnUser(shell, title, message, style);
	}

	private void submitEnd() {
		String title = TSEMessages.get("success.title");
		String message = TSEMessages.get("submit.success");
		int style = SWT.ICON_INFORMATION;
		
		Warnings.warnUser(shell, title, message, style);
	}
	
	private Message getUnsupportedOpWarning(ReportSendOperation operation) {
		
		String datasetId = operation.getDataset().getId();

		String title = TSEMessages.get("error.title");
		String message;
		boolean fatal = false;
		
		switch(operation.getStatus()) {
		case ACCEPTED_DWH:
			message = TSEMessages.get("send.warning.acc.dwh", datasetId);
			break;
		case SUBMITTED:
			message = TSEMessages.get("send.warning.submitted", datasetId);
			break;
		case PROCESSING:
			message = TSEMessages.get("send.warning.processing", datasetId);
			break;
		default:
			message = TSEMessages.get("send.error.acc.dcf", PropertiesReader.getSupportEmail());
			fatal = true;
			break;
		}
		
		return fatal ? Warnings.createFatal(message) : Warnings.create(title, message, SWT.ICON_ERROR);
	}
	
	/**
	 * Warning based on the required operation and on the status of the dataset
	 * @param shell
	 * @param operation
	 * @return
	 */
	public boolean showSendWarning(Shell shell, ReportSendOperation operation) {
		
		boolean goOn = true;
		
		String title = null;
		String message = null;
		int style = SWT.ICON_ERROR;
		boolean needConfirmation = false;
		
		// new dataset
		if (operation.getStatus() == null)
			return true;
		
		String datasetId = operation.getDataset().getId();
		
		switch(operation.getStatus()) {
		case ACCEPTED_DWH:
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("send.warning.acc.dwh", datasetId);
			goOn = false;
			break;
		case SUBMITTED:
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("send.warning.submitted", datasetId);
			goOn = false;
			break;
		case PROCESSING:
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("send.warning.processing", datasetId);
			goOn = false;
			break;
		case REJECTED_EDITABLE:
		case VALID:
		case VALID_WITH_WARNINGS:
			
			title = TSEMessages.get("warning.title");
			message = TSEMessages.get("send.warning.replace", datasetId, operation.getStatus().getLabel());
			style = SWT.YES | SWT.NO | SWT.ICON_WARNING;
			needConfirmation = true;
			break;
		case REJECTED:
		case DELETED:
			// Do nothing, just avoid the default case
			goOn = true;

			break;
		default:
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("send.error.acc.dcf", PropertiesReader.getSupportEmail());
			goOn = false;
			break;
		}
		
		if (title != null && message != null) {
			
			int val = Warnings.warnUser(shell, title, message, style);
			
			// if the caller need confirmation
			if (needConfirmation) {
				
				goOn = val == SWT.YES;
			}
		}
		
		// default answer is no
		return goOn;
	}

	@Override
	public boolean askReplaceConfirmation(ReportSendOperation sendOp) {
		
		String datasetId = sendOp.getDataset().getId();
		String status = sendOp.getDataset().getRCLStatus().getLabel();
		
		String title = TSEMessages.get("warning.title");
		String message = TSEMessages.get("send.warning.replace", datasetId, status);
		int style = SWT.YES | SWT.NO | SWT.ICON_WARNING;
		
		int val = Warnings.warnUser(shell, title, message, style);
		
		return val == SWT.YES;
	}

	@Override
	public boolean askConfirmation(ReportAction action) {
		
		String title = TSEMessages.get("warning.title");
		String message = null;
		switch(action) {
		case SUBMIT:
			message = TSEMessages.get("submit.confirm");
			break;
		case REJECT:
			message = TSEMessages.get("reject.confirm");
			break;
		case SEND:
			message = TSEMessages.get("send.confirm");
			break;
		case AMEND:
			message = TSEMessages.get("amend.confirm");
			break;
		default:
			break;
		}
		
		if (message == null)
			return false;
		
		int val = Warnings.warnUser(shell, title, 
				message,
				SWT.ICON_WARNING | SWT.YES | SWT.NO);
		
		return val == SWT.YES;
	}

	@Override
	public boolean askDataCollectionConfirmation(Report report) {
		
		String dc = PropertiesReader.getDataCollectionCode(report.getYear());
		int val = Warnings.warnUser(shell, TSEMessages.get("warning.title"), 
				TSEMessages.get("send.confirm.dc", dc),
				SWT.ICON_WARNING | SWT.YES | SWT.NO);
		
		return val == SWT.YES;
	}
}
