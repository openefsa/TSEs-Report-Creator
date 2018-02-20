package report_downloader;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;

import app_config.AppPaths;
import dataset.Dataset;
import dataset.DatasetList;
import dataset.DcfDatasetStatus;
import dataset.IDataset;
import dataset.NoAttachmentException;
import formula.FormulaException;
import mocks.TableDaoMock;
import providers.FormulaService;
import providers.IFormulaService;
import providers.ITableDaoService;
import providers.TableDaoService;
import providers.TseReportService;
import soap.DetailedSOAPException;
import soap_test.GetAckMock;
import soap_test.GetDatasetMock;
import soap_test.GetDatasetsListMock;
import soap_test.SendMessageMock;
import table_skeleton.TableRow;
import table_skeleton.TableRowList;
import tse_config.CustomStrings;
import xlsx_reader.TableSchemaList;

public class ReportImporterTest {
	
	private TseReportService reportService;
	private GetAckMock getAck;
	private GetDatasetsListMock<IDataset> getDatasetsList;
	private SendMessageMock sendMessage;
	private GetDatasetMock getDataset;
	private ITableDaoService daoService;
	private IFormulaService formulaService;
	
	@Before
	public void init() {
		
		this.getAck = new GetAckMock();
		this.getDatasetsList = new GetDatasetsListMock<>();
		this.sendMessage = new SendMessageMock();
		this.getDataset = new GetDatasetMock();
		this.daoService = new TableDaoService(new TableDaoMock());
		
		this.formulaService = new FormulaService(daoService);
		
		this.reportService = new TseReportService(getAck, getDatasetsList, sendMessage, getDataset,
				daoService, formulaService);
	}
	
	@Test
	public void importFirstVersionOfReport() throws DetailedSOAPException, XMLStreamException, 
		IOException, FormulaException, NoAttachmentException, ParseException {
		
		String datasetId = "11920";
		
		getDataset.addDatasetFile(datasetId, new File("test-files" 
				+ System.getProperty("file.separator") + "dataset-first-version.xml"));
		
		DatasetList datasetVersions = new DatasetList();
		
		Dataset d = new Dataset();
		
		// note: these information come from the get datasets list
		// in the normal process flow
		d.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		d.setId(datasetId);
		d.setSenderId("AT1706.00");
		
		datasetVersions.add(d);
		
		// import the file
		TseReportImporter imp = new TseReportImporter(datasetVersions, reportService, daoService);
		imp.importReport();
		
		// check contents of the file with what was imported
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).size());
		assertEquals(3, daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET)).size());
		assertEquals(3, daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET)).size());
		assertEquals(5, daoService.getAll(TableSchemaList.getByName(CustomStrings.RESULT_SHEET)).size());
		
		// get the objects
		TableRow report = daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).iterator().next();
		TableRowList summInfos = daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));
		TableRowList cases = daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));
		TableRowList results = daoService.getAll(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
		
		for(TableRow si: summInfos) {
			
			// under the same report
			assertEquals(String.valueOf(report.getDatabaseId()), si.getCode(CustomStrings.REPORT_ID_COL));
			
			// check contents
			if (si.getLabel(CustomStrings.RES_ID_COLUMN).equals("0404_000069.0")) {
				assertEquals("0404_000069", si.getLabel(CustomStrings.RESULT_PROG_ID));
			}
			if (si.getLabel(CustomStrings.RES_ID_COLUMN).equals("0404_000068.0")) {
				assertEquals("0404_000068", si.getLabel(CustomStrings.RESULT_PROG_ID));
			}
		}
		
		//TODO check other contents
	}
	
	@Test
	public void importNonFirstVersionInValidState() {
		
	}
	
	@Test
	public void importNonFirstVersionInAcceptedDwhState() throws DetailedSOAPException, XMLStreamException, 
		IOException, FormulaException, NoAttachmentException, ParseException {
		
		String firstVersionId = "11513";
		String amendedVersionId = "11518";
		
		// prepare the get dataset request
		getDataset.addDatasetFile(firstVersionId, new File("test-files" 
				+ System.getProperty("file.separator") + "BE1011.00.xml"));
		
		getDataset.addDatasetFile(amendedVersionId, new File("test-files" 
				+ System.getProperty("file.separator") + "BE1011.01.xml"));
		
		DatasetList datasetVersions = new DatasetList();
		
		// note: these information come from the get datasets list
		// in the normal process flow
		Dataset d = new Dataset();
		d.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		d.setId(firstVersionId);
		d.setSenderId("BE1011.00");
		
		Dataset d2 = new Dataset();
		d2.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		d2.setId(amendedVersionId);
		d2.setSenderId("BE1011.01");
		
		datasetVersions.add(d);
		datasetVersions.add(d2);
		
		// import the file
		TseReportImporter imp = new TseReportImporter(datasetVersions, reportService, daoService);
		imp.importReport();

		// check contents of the file with what was imported
		// only the merged version is present
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).size());
		assertEquals(6, daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET)).size());
		assertEquals(17, daoService.getAll(TableSchemaList.getByName(CustomStrings.RESULT_SHEET)).size());
	}
}