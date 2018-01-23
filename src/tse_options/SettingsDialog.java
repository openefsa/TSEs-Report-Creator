package tse_options;

import java.io.IOException;
import java.util.Collection;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXException;

import app_config.PropertiesReader;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import message.SendMessageException;
import message_creator.OperationType;
import report.ReportException;
import soap.MySOAPException;
import table_dialog.DialogBuilder;
import table_dialog.RowValidatorLabelProvider;
import table_skeleton.TableCell;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_config.GeneralWarnings;
import tse_report.TseReport;
import tse_validator.SimpleRowValidatorLabelProvider;
import user.User;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Dialog which is used to set global settings for the application
 * @author avonva
 *
 */
public class SettingsDialog extends OptionsDialog {

	public static final String WINDOW_CODE = "Settings";
	
	public SettingsDialog(Shell parent) {
		super(parent, TSEMessages.get("settings.title"), WINDOW_CODE);
	}

	@Override
	public String getSchemaSheetName() {
		return CustomStrings.SETTINGS_SHEET;
	}
	
	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {

		boolean closeWindow = super.apply(schema, rows, selectedRow);
		
		TableRow settings = rows.iterator().next();
		
		if (settings == null)
			return closeWindow;
		
		login(settings);
		
		return closeWindow;
	}
	
	private boolean login(TableRow settings) {
		
		// get credentials
		TableCell usernameVal = settings.get(CustomStrings.SETTINGS_USERNAME);
		TableCell passwordVal = settings.get(CustomStrings.SETTINGS_PASSWORD);

		if (usernameVal == null || passwordVal == null)
			return false;

		// login the user
		String username = usernameVal.getLabel();
		String password = passwordVal.getLabel();

		User.getInstance().login(username, password);
		
		return true;
	}
	
	@Override
	public Menu createMenu() {
		
		Menu menu = new Menu(getDialog());
		MenuItem test = new MenuItem(menu, SWT.PUSH);
		test.setText(TSEMessages.get("settings.test.connection"));
		
		// check if it is possible to make a get dataset
		// list request and possibly download the
		// test report if present. If not present, it
		// creates the test report and send it to the dcf
		test.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				testConnection();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		return menu;
	}

	@Override
	public TableRow createNewRow(TableSchema schema, Selection type) {
		return null;
	}

	@Override
	public void processNewRow(TableRow row) {}
	
	@Override
	public RowValidatorLabelProvider getValidator() {
		return new SimpleRowValidatorLabelProvider();
	}

	/**
	 * Test the connection with the inserted credentials
	 */
	private void testConnection() {
		
		System.out.println("Test connection: started");
		
		getPanelBuilder().selectRow(0);
		
		TableRow settings = getRows().iterator().next();

		if (settings == null || !settings.areMandatoryFilled()) {
			Warnings.warnUser(getDialog(), 
					TSEMessages.get("error.title"), 
					TSEMessages.get("settings.test.connection.warning"));
			return;
		}

		// login the user if not done before
		boolean ok = login(getSelection());
		
		if (!ok)
			return;
		
		// change the cursor to wait
		getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
		
		String title = null;
		String message = null;
		int jStyle = JOptionPane.ERROR_MESSAGE;

		TseReport report = null;
		try {
			
			report = TseReport.createDefault();
			
			// save report in db in order to perform send
			report.save();
			
			report.exportAndSend(OperationType.TEST);

			// here is success
			title = TSEMessages.get("success.title");
			message = TSEMessages.get("test.connection.success");
			jStyle = JOptionPane.INFORMATION_MESSAGE;
			
		} catch (MySOAPException e) {

			e.printStackTrace();
			
			System.err.println("Test connection: failed.");

			String[] warnings = Warnings.getSOAPWarning(e);
			title = warnings[0];
			message = warnings[1];
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("test.connection.fail3", 
					PropertiesReader.getSupportEmail(), e.getMessage());
		} catch (SendMessageException e) {
			
			// here we got TRXKO
			e.printStackTrace();
			
			String[] warning = GeneralWarnings.getSendMessageWarning(e);
			title = warning[0];
			message = warning[1];
			
		} catch (ReportException e) {
			// There an invalid operation was used
			e.printStackTrace();
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("test.connection.fail2", 
					PropertiesReader.getSupportEmail(), e.getMessage());
		}
		finally {
			
			// change the cursor to old cursor
			getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
			
			// delete the report from the db
			if (report != null)
				report.delete();
		}
		
		// if we have an error message stop and show the error
		if (message != null) {
			
			final JDialog dialog = new JDialog();
			dialog.setAlwaysOnTop(true);
			JOptionPane.showMessageDialog(dialog, message, title, jStyle);
			dialog.dispose();
			
			//Warnings.warnUser(getDialog(), title, message, style);
		}
	}
	
	@Override
	public void addWidgets(DialogBuilder viewer) {
		
		MouseListener listener = new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent arg0) {
				viewer.setPasswordVisibility(CustomStrings.SETTINGS_PASSWORD, false);
			}
			
			@Override
			public void mouseDown(MouseEvent arg0) {
				viewer.setPasswordVisibility(CustomStrings.SETTINGS_PASSWORD, true);
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {}
		};
		
		SelectionAdapter testConnectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				testConnection();
			}
		};
		
		// add image to edit button
		Image testConnImg = new Image(getDialog().getDisplay(), this.getClass()
				.getClassLoader().getResourceAsStream("test-connection.png"));
		
		Image showPwdImg = new Image(getDialog().getDisplay(), this.getClass()
				.getClassLoader().getResourceAsStream("show-password.png"));
		
		viewer.addHelp(TSEMessages.get("settings.help.title"))
			.addGroup("toolbar", TSEMessages.get("si.toolbar.title"), new GridLayout(2, false), null)
			.addButtonToComposite("showPwdBtn", "toolbar", TSEMessages.get("settings.show.password"), listener)
			.addButtonImage("showPwdBtn", showPwdImg)
			.addButtonToComposite("testConBtn", "toolbar", TSEMessages.get("settings.test.connection"), testConnectionListener)
			.addButtonImage("testConBtn", testConnImg)
			.addTable(CustomStrings.SETTINGS_SHEET, true);
	}
}
