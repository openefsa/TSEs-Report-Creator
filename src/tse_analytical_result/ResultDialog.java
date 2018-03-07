package tse_analytical_result;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import dataset.RCLDatasetStatus;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import predefined_results.PredefinedResult;
import predefined_results.PredefinedResultHeader;
import providers.IFormulaService;
import providers.ITableDaoService;
import providers.PredefinedResultService;
import providers.TseReportService;
import report.Report;
import session_manager.TSERestoreableWindowDao;
import table_dialog.DialogBuilder;
import table_dialog.EditorListener;
import table_dialog.RowValidatorLabelProvider;
import table_relations.Relation;
import table_skeleton.TableColumn;
import table_skeleton.TableRow;
import table_skeleton.TableRowList;
import tse_case_report.CaseReport;
import tse_components.TableDialogWithMenu;
import tse_config.CustomStrings;
import tse_summarized_information.SummarizedInfo;
import tse_validator.ResultValidator;
import window_restorer.RestoreableWindow;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;
import xml_catalog_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class ResultDialog extends TableDialogWithMenu {
	
	private static final Logger LOGGER = LogManager.getLogger(ResultDialog.class);
	
	private RestoreableWindow window;
	private static final String WINDOW_CODE = "AnalyticalResult";
	
	private TseReportService reportService;
	private ITableDaoService daoService;
	private IFormulaService formulaService;
	
	private Report report;
	private SummarizedInfo summInfo;
	private CaseReport caseInfo;
	
	public ResultDialog(Shell parent, Report report, SummarizedInfo summInfo, 
			CaseReport caseInfo, TseReportService reportService, ITableDaoService daoService,
			IFormulaService formulaService) {
		
		super(parent, TSEMessages.get("result.title"), true, false);
		
		this.report = report;
		this.summInfo = summInfo;
		this.caseInfo = caseInfo;
		this.reportService = reportService;
		this.daoService = daoService;
		this.formulaService = formulaService;
		
		// create the dialog
		super.create();
		
		this.window = new RestoreableWindow(getDialog(), WINDOW_CODE);
		boolean restored = window.restore(TSERestoreableWindowDao.class);
		window.saveOnClosure(TSERestoreableWindowDao.class);
		
		// add 300 px in height
		if (!restored)
			addHeight(300);
		
		setEditorListener(new EditorListener() {
			
			@Override
			public void editStarted() {}
			
			@Override
			public void editEnded(TableRow row, TableColumn field, boolean changed) {
				
				// update the base term and the result value if
				// the test aim was changed
				if (changed && (field.getId().equals(CustomStrings.TEST_AIM_COL) 
						|| field.getId().equals(CustomStrings.AN_METH_CODE_COL))) {

					// if genotyping set base term
					if (row.getCode(CustomStrings.AN_METH_CODE_COL).equals(CustomStrings.AN_METH_CODE_GENOTYPING)
							&& !summInfo.getCode(CustomStrings.SUMMARIZED_INFO_TYPE)
								.equals(CustomStrings.SUMMARIZED_INFO_BSEOS_TYPE)) {

						try {
							
							PredefinedResultService r = new PredefinedResultService(daoService, formulaService);
							
							PredefinedResult predRes = r.getPredefinedResult(report, summInfo, caseInfo);

							row.put(CustomStrings.PARAM_CODE_BASE_TERM_COL, 
									predRes.get(PredefinedResultHeader.GENOTYPING_BASE_TERM));
							
						} catch (IOException e) {
							e.printStackTrace();
							LOGGER.error("Cannot fill results field=" 
									+ CustomStrings.PARAM_CODE_BASE_TERM_COL + " using the predefined results", e);
						}
					}
					else {
						if (!row.getCode(CustomStrings.TEST_AIM_COL).isEmpty())
							PredefinedResultService.addParamAndResult(row, row.getCode(field.getId()));
					}
				}
				
				// reset the aim of the test if the test type is changed
				if (changed && field.equals(CustomStrings.AN_METH_TYPE_COL)) {
					
					TableRow completeRow = getPanelBuilder().getTable().getCompleteRow(row.getDatabaseId());
					
					completeRow.remove(CustomStrings.TEST_AIM_COL);
					completeRow.remove(CustomStrings.AN_METH_CODE_COL);
					row.remove(CustomStrings.TEST_AIM_COL);
					row.remove(CustomStrings.AN_METH_CODE_COL);

					completeRow.update();
				}
			}
		});
		
		updateUI();
	}
	
	public void askForDefault() {
			
		// create default if no results are present
		if (!reportService.hasChildren(caseInfo, TableSchemaList.getByName(CustomStrings.RESULT_SHEET)) 
				&& isEditable() && !this.summInfo.isBSEOS()) {

			int val = Warnings.warnUser(getDialog(), TSEMessages.get("warning.title"), 
					TSEMessages.get("result.confirm.default"),
					SWT.YES | SWT.NO | SWT.ICON_QUESTION);
			
			LOGGER.info("Add default results to the list? " + (val == SWT.YES));
			
			if (val == SWT.NO)
				return;
			
			try {
				
				TableRowList results = reportService.createDefaultResults(report, summInfo, caseInfo);
				this.setRows(results);
				
				warnUser(TSEMessages.get("warning.title"), 
						TSEMessages.get("result.check.default"),
						SWT.ICON_WARNING);

				LOGGER.info("Default results created");
				
			} catch (IOException e) {
				e.printStackTrace();
				LOGGER.error("Cannot create predefined results for case with sampId=" 
				+ caseInfo.getCode(CustomStrings.SAMPLE_ID_COL), e);
			}
		}
	}
	
	/**
	 * make table non editable if needed
	 */
	private void updateUI() {
		
		LOGGER.info("Updating GUI");
		
		DialogBuilder panel = getPanelBuilder();
		String status = report.getLabel(AppPaths.REPORT_STATUS);
		RCLDatasetStatus datasetStatus = RCLDatasetStatus.fromString(status);
		boolean editableReport = datasetStatus.isEditable();
		panel.setTableEditable(editableReport);
		panel.setRowCreatorEnabled(editableReport);
		
		LOGGER.info("GUI updated");
	}

	/**
	 * Create a new row with default values
	 * @param element
	 * @return
	 * @throws IOException 
	 */
	@Override
	public TableRow createNewRow(TableSchema schema, Selection element) {
		
		LOGGER.info("Creating a new result");
		
		TableRow row = new TableRow(schema);
		
		Relation.injectParent(report, row);
		Relation.injectParent(summInfo, row);
		Relation.injectParent(caseInfo, row);
		
		return row;
	}

	@Override
	public String getSchemaSheetName() {
		return CustomStrings.RESULT_SHEET;
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
		return new ResultValidator();
	}
	
	@Override
	public Menu createMenu() {
		Menu menu = super.createMenu();
		addRemoveMenuItem(menu);
		return menu;
	}

	@Override
	public void addWidgets(DialogBuilder viewer) {
		
		String sampleId = caseInfo.getLabel(CustomStrings.SAMPLE_ID_COL);
		String animalId = caseInfo.getLabel(CustomStrings.ANIMAL_ID_COL);
		String caseId = caseInfo.getLabel(CustomStrings.NATIONAL_CASE_ID_COL);
		
		String sampleIdRow = TSEMessages.get("result.sample.id", sampleId);
		String animalIdRow = TSEMessages.get("result.animal.id", animalId);
		String caseIdRow = TSEMessages.get("result.case.id", caseId);
		
		viewer.addHelp(TSEMessages.get("result.help.title"))
			.addRowCreator(TSEMessages.get("result.add.record"))
			.addLabel("sampLabel", sampleIdRow.toString())
			.addLabel("animalLabel", animalIdRow.toString())
			.addLabel("caseIdLabel", caseIdRow.toString())
			.addTable(CustomStrings.RESULT_SHEET, true, report, summInfo, caseInfo);  // add parent to be able to solve isVisible field
	}
}
