/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.ssp.web.api.reports; // NOPMD

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

import org.jasig.ssp.model.Person;
import org.jasig.ssp.model.external.RegistrationStatusByTerm;
import org.jasig.ssp.model.external.Term;
import org.jasig.ssp.model.reference.Campus;
import org.jasig.ssp.model.reference.ProgramStatus;
import org.jasig.ssp.security.permissions.Permission;
import org.jasig.ssp.service.CaseloadService;
import org.jasig.ssp.service.EarlyAlertResponseService;
import org.jasig.ssp.service.EarlyAlertService;
import org.jasig.ssp.service.JournalEntryService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.ReferenceService;
import org.jasig.ssp.service.SecurityService;
import org.jasig.ssp.service.TaskService;
import org.jasig.ssp.service.external.ExternalPersonService;
import org.jasig.ssp.service.external.RegistrationStatusByTermService;
import org.jasig.ssp.service.external.TermService;
import org.jasig.ssp.service.reference.CampusService;
import org.jasig.ssp.service.reference.ProgramStatusService;
import org.jasig.ssp.service.reference.ReferralSourceService;
import org.jasig.ssp.service.reference.StudentTypeService;
import org.jasig.ssp.transferobject.reports.CaseLoadActivityReportTO;
import org.jasig.ssp.transferobject.reports.CaseLoadReportTO;
import org.jasig.ssp.transferobject.reports.EarlyAlertStudentReportTO;
import org.jasig.ssp.transferobject.reports.EarlyAlertTermCaseCountsTO;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.jasig.ssp.web.api.AbstractBaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Maps;

/**
 * Service methods for Reporting on Early Alert Case Counts
 * <p>
 * Mapped to URI path <code>/1/report/earlyalertcasecounts</code>
 */
@Controller
@RequestMapping("/1/report/earlyalertcasecounts")
public class EarlyAlertCaseCountsReportController extends EarlyAlertReportBaseController {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(CaseloadActivityReportController.class);
	
	@Autowired
	protected transient TermService termService;
	
	@Autowired
	protected transient ExternalPersonService externalPersonService;
	
	@Autowired
	protected transient PersonService personService;
	
	@Autowired
	protected transient CampusService campusService;
	
	@Autowired
	protected transient RegistrationStatusByTermService registrationStatusByTermService;

	@Autowired
	protected transient EarlyAlertService earlyAlertService;
	@Autowired
	protected transient EarlyAlertResponseService earlyAlertResponseService;

	
	@InitBinder
	public void initBinder(final WebDataBinder binder) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy",
				Locale.US);
		dateFormat.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(
				dateFormat, true));
	}
	
	
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize(Permission.SECURITY_REPORT_READ)
	public @ResponseBody
	void getEarlyAlertCaseCountsReport(
			final HttpServletResponse response,		
			final @RequestParam(required = false) UUID campusId,
			final @RequestParam(required = false) List<String> termCodes,			
			final @RequestParam(required = false, defaultValue = "pdf") String reportType)
			throws ObjectNotFoundException, JRException, IOException {
				
		final Map<String, Object> parameters = Maps.newHashMap();
		
		Campus campus = null;		
		if(campusId != null){
			campus = campusService.get(campusId);
			parameters.put("campus", campus.getName());
		}	

		final ArrayList<EarlyAlertTermCaseCountsTO> caseLoads = new ArrayList<EarlyAlertTermCaseCountsTO>();
		ArrayList<String> termCodesList = new ArrayList<String>();
		ArrayList<String> termNamesList = new ArrayList<String>();
		
		if(termCodes != null){
			for(String termCode:termCodes)
			{
				final Term term = termService.getByCode(termCode);
				termCodesList.add(term.getCode());
				termNamesList.add(term.getName());
				
				EarlyAlertTermCaseCountsTO caseCounts = new EarlyAlertTermCaseCountsTO(term.getCode(),
						term.getName(),
						earlyAlertService.getCountOfEarlyAlertStudentsByDate(term.getStartDate(), 
								term.getEndDate(), campus),
						earlyAlertService.getCountOfEarlyAlertsByCreatedDate(term.getStartDate(), 
								term.getEndDate(), campus),
						earlyAlertResponseService.getEarlyAlertRespondedToCount(term.getStartDate(), 
										term.getEndDate(), campus),
						earlyAlertService.getCountOfEarlyAlertsClosedByDate(term.getStartDate(), 
												term.getEndDate(), campus)
										);
			
				caseLoads.add(caseCounts);
	
			}
			parameters.put("termCodes", concatNamesFromStrings(termCodesList) );
			parameters.put("termNames", concatNamesFromStrings(termNamesList));
		}else{
			
			EarlyAlertTermCaseCountsTO caseCounts = new EarlyAlertTermCaseCountsTO("All",
					"All",
					earlyAlertService.getCountOfEarlyAlertStudentsByDate(null, null, campus),
					earlyAlertService.getCountOfEarlyAlertsByCreatedDate(null, null, campus),
					earlyAlertResponseService.getEarlyAlertRespondedToCount(null, null, campus),
					earlyAlertService.getCountOfEarlyAlertsClosedByDate(null, null, campus)
									);
		
			caseLoads.add(caseCounts);
			
			parameters.put("termCodes", "All" );
			parameters.put("termNames", "All");
		}
		
		
				
		generateReport( response,  parameters, caseLoads,  "/reports/earlyAlertCaseCounts.jasper", 
				 reportType, "Early_Alert_Case_Counts_Report");
	}

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}
	
}