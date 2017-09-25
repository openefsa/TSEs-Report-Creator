package tse_analytical_result;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.swt.widgets.Shell;

import table_dialog.RowValidatorLabelProvider;
import table_dialog.TableViewWithHelp.RowCreationMode;
import table_skeleton.TableRow;
import tse_components.TableDialogWithMenu;
import tse_config.CustomPaths;
import tse_validator.SimpleRowValidatorLabelProvider;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class ResultDialog extends TableDialogWithMenu {
	
	public ResultDialog(Shell parent, TableRow report, TableRow summInfo) {
		
		super(parent, "Analytical results", "Analytical results", 
				true, RowCreationMode.STANDARD, true, false);
		
		// add 300 px in height
		addDialogHeight(300);
		
		// specify title and list of the selector
		setRowCreatorLabel("Add result:");
		
		// add the parents
		addParentTable(report);
		addParentTable(summInfo);
	}
	
	@Override
	public void setParentFilter(TableRow parentFilter) {
		setRowCreationEnabled(parentFilter != null);
		super.setParentFilter(parentFilter);
	}

	/**
	 * Create a new row with default values
	 * @param element
	 * @return
	 * @throws IOException 
	 */
	@Override
	public TableRow createNewRow(TableSchema schema, Selection element) {
		
		TableRow row = new TableRow(schema);
		return row;
	}

	@Override
	public String getSchemaSheetName() {
		return CustomPaths.RESULT_SHEET;
	}

	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		return true;
	}

	@Override
	public Collection<TableRow> loadInitialRows(TableSchema schema, TableRow parentFilter) {
		return null;
	}

	@Override
	public void processNewRow(TableRow row) {}
	
	@Override
	public RowValidatorLabelProvider getValidator() {
		return new SimpleRowValidatorLabelProvider();
	}
}