/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.ssp.service.impl;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.dao.MapStatusReportDao;
import org.jasig.ssp.dao.PersonAssocAuditableCrudDao;
import org.jasig.ssp.dao.external.ExternalSubstitutableCourseDao;
import org.jasig.ssp.model.*;
import org.jasig.ssp.model.external.*;
import org.jasig.ssp.service.AbstractPersonAssocAuditableService;
import org.jasig.ssp.service.MapStatusReportService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PlanService;
import org.jasig.ssp.service.external.ExternalStudentTranscriptCourseService;
import org.jasig.ssp.service.external.ExternalStudentTranscriptNonCourseEntityService;
import org.jasig.ssp.service.external.TermService;
import org.jasig.ssp.service.reference.ConfigService;
import org.jasig.ssp.transferobject.reports.MapPlanStatusReportCourse;
import org.jasig.ssp.transferobject.reports.MapStatusReportOwnerAndCoachInfo;
import org.jasig.ssp.transferobject.reports.MapStatusReportPerson;
import org.jasig.ssp.transferobject.reports.MapStatusReportSummaryDetail;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;


/**
 * Person service implementation
 * 
 * @author tony.arland
 */
@Service
@Transactional
public class MapStatusReportServiceImpl extends AbstractPersonAssocAuditableService<MapStatusReport>
		implements MapStatusReportService {

	@Autowired 
	private MapStatusReportDao dao;

	@Autowired
	private transient PlanService planService;
	
	@Autowired 
	private transient ExternalStudentTranscriptCourseService externalStudentTranscriptCourseService;

    @Autowired
    private transient ExternalStudentTranscriptNonCourseEntityService externalStudentTranscriptNonCourseEntityService;
	
	@Autowired 
	private transient TermService termService;

	@Autowired
	private transient ConfigService configService;
	
	@Autowired 
	private transient ExternalSubstitutableCourseDao externalSubstitutableCourseDao;


	private static String CONFIGURABLE_MATCH_CRITERIA_COURSE_TITLE = "COURSE_TITLE";
	
	private static String CONFIGURABLE_MATCH_CRITERIA_CREDIT_HOURS = "CREDIT_HOURS";
	
	private static String CONFIGURABLE_MATCH_CRITERIA_COURSE_CODE = "COURSE_CODE";

	private static String CONFIGURABLE_MATCH_COURSE_ANY_PASSING_COURSE = "map_plan_status_use_any_passing_course";

	private static final Logger LOGGER = LoggerFactory.getLogger(MapStatusReportServiceImpl.class);


	@Override
	public MapStatusReport save(MapStatusReport obj)
			throws ObjectNotFoundException, ValidationException {
		return dao.save(obj);
	}

	@Override
	public void deleteAllOldReports() {
		dao.deleteAllOldReports();
	}

	@Override
	protected PersonAssocAuditableCrudDao<MapStatusReport> getDao() {
		return dao;
	}
	
	@Override
	public MapStatusReport evaluatePlan(Set<String> gradesSet, 
			Set<String> criteriaSet,
			Term cutoffTerm,  
			List<Term> allTerms,
			MapStatusReportPerson planAndPersonInfo,
			Collection<ExternalSubstitutableCourse> allSubstitutableCourses,
            Collection<ExternalStudentTranscriptNonCourseEntity> nonCourseEntities,
			List<ExternalStudentTranscriptCourse> transcript,
			boolean termBound, 
			boolean useSubstitutableCourses) {
		LOGGER.info("Loading plan with id {}",planAndPersonInfo.getPlanId());
		
		List<MapPlanStatusReportCourse> planCourses = planService.getAllPlanCoursesForStatusReport(planAndPersonInfo.getPlanId()); 
		LOGGER.info("Loading plan "+planAndPersonInfo.getPlanId()+" courses with with count {}",planCourses.size());

        final MapStatusReport report = initReport(planAndPersonInfo);
		
		//We only add something to course details in the case where there is an anomaly 
		List<MapStatusReportCourseDetails> reportCourseDetails = new ArrayList<MapStatusReportCourseDetails>();
		
		//We will create an entry for all plan terms prior to the cutoff term, regardless if anomaly is present
		List<MapStatusReportTermDetails> reportTermDetails = new ArrayList<MapStatusReportTermDetails>();
		
		//Create an entry whenever there is a term or course substitution
		List<MapStatusReportSubstitutionDetails> reportSubstitutionDetails = new ArrayList<MapStatusReportSubstitutionDetails>();

        //Create an entry whenever there is a course override
        List<MapStatusReportOverrideDetails> reportOverrideDetails = new ArrayList<MapStatusReportOverrideDetails>();

        //Organize Plan Courses by term.. Preprocessing this data by term helps with term based matching
		Map<String,List<MapPlanStatusReportCourse>> planCoursesByTerm = organizePlanCoursesByTerm(planCourses);
		
		//Organize Transcript Courses by Term  Preprocessing this data by term helps with term based matching
		Map<String,List<ExternalStudentTranscriptCourse>> transcriptCoursesByTerm = organizeTranscriptCourseByTerm(transcript);
		
		Map<String,List<MapStatusReportCourseDetails>> courseReportsByTerm = new HashMap<String,List<MapStatusReportCourseDetails>>();

	    //Iterate through terms, if there are plan courses for a particular term, start status calculation
		for (Term term : allTerms) {
            if (term.getStartDate().before(cutoffTerm.getStartDate()) || term.getStartDate().equals(cutoffTerm.getStartDate())) {
                List<MapPlanStatusReportCourse> planCoursesForTerm = planCoursesByTerm.get(term.getCode());
                if (planCoursesForTerm != null && !planCoursesForTerm.isEmpty()) {
                    courseReportsByTerm.put(term.getCode(),new ArrayList<MapStatusReportCourseDetails>());
                    evaluateTerm(gradesSet, criteriaSet, report,
                            reportCourseDetails, transcriptCoursesByTerm,
                            term, planCoursesForTerm,courseReportsByTerm,transcript,allSubstitutableCourses,
                            reportSubstitutionDetails,nonCourseEntities,reportOverrideDetails,planAndPersonInfo);
                }
            }
		}
		
		//Build the term part of the report
		buildTermDetails(report, reportTermDetails, courseReportsByTerm, planCoursesByTerm);
		
		report.setPlanStatus(calculatePlanStatus(reportCourseDetails, reportSubstitutionDetails, reportOverrideDetails, termBound, useSubstitutableCourses));
		report.setPlanRatio(calculatePlanRatio(report, planCourses, reportCourseDetails, reportSubstitutionDetails, reportOverrideDetails, termBound, useSubstitutableCourses, planCoursesByTerm, reportTermDetails));
		report.setCourseDetails(reportCourseDetails);
        report.setTermDetails(reportTermDetails);
		report.setSubstitutionDetails(reportSubstitutionDetails);
        report.setOverrideDetails(reportOverrideDetails);
		report.setPlanNote(generatePlanNote(report));

        return report;
	}

	private PlanStatus calculatePlanStatus(List<MapStatusReportCourseDetails> reportCourseDetails,
                                           List<MapStatusReportSubstitutionDetails> reportSubstitutionDetails,
                                           List<MapStatusReportOverrideDetails> reportOverrideDetails,
                                           boolean termBound, boolean useSubstitutableCourses) {

		if (reportCourseDetails.size() > 0 ) {
            return PlanStatus.OFF;
        }
		
		if (termBound == true && useSubstitutableCourses == true) {
			if (containsTermSubstitution(reportSubstitutionDetails)) {
                return PlanStatus.OFF;
            }
			if (containsCourseSubstitution(reportSubstitutionDetails)) {
                return PlanStatus.ON_TRACK_SUBSTITUTION;
            }
		}

        if (termBound == false && useSubstitutableCourses == false) {
			if (containsTermSubstitution(reportSubstitutionDetails)) {
                return PlanStatus.ON_TRACK_SEQUENCE;
            }
			if (containsCourseSubstitution(reportSubstitutionDetails)) {
                return PlanStatus.OFF;
            }
		}

		if (termBound == true && useSubstitutableCourses == false) {
			if (containsTermSubstitution(reportSubstitutionDetails)) {
                return PlanStatus.OFF;
            }
			if (containsCourseSubstitution(reportSubstitutionDetails)) {
                return PlanStatus.OFF;
            }
		}

        if (termBound == false && useSubstitutableCourses == true) {
			if (containsTermSubstitution(reportSubstitutionDetails)) {
                return PlanStatus.ON_TRACK_SEQUENCE;
            }
			if (containsCourseSubstitution(reportSubstitutionDetails)) {
                return PlanStatus.ON_TRACK_SUBSTITUTION;
            }
		}

        if (reportOverrideDetails.size() > 0) {
            return PlanStatus.ON_TRACK_OVERRIDE;
        }

        return PlanStatus.ON;
	}	
	
	private boolean containsTermSubstitution(List<MapStatusReportSubstitutionDetails> reportSubstitutionDetails) {
		for (MapStatusReportSubstitutionDetails mapStatusReportSubstitutionDetails : reportSubstitutionDetails) {
			if (SubstitutionCode.TERM.equals(mapStatusReportSubstitutionDetails.getSubstitutionCode())) {
                return true;
            }
		}

		return false;
	}

	private int numTermSubstitution(List<MapStatusReportSubstitutionDetails> reportSubstitutionDetails) {
		int result = 0;
		for (MapStatusReportSubstitutionDetails mapStatusReportSubstitutionDetails : reportSubstitutionDetails) {
			if (SubstitutionCode.TERM.equals(mapStatusReportSubstitutionDetails.getSubstitutionCode())) {
                result++;
            }
		}

		return result;
	}
	
	private int numCourseSubstitution(List<MapStatusReportSubstitutionDetails> reportSubstitutionDetails) {
		int result = 0;
		for (MapStatusReportSubstitutionDetails mapStatusReportSubstitutionDetails : reportSubstitutionDetails) {
			if (SubstitutionCode.SUBSTITUTABLE_COURSE.equals(mapStatusReportSubstitutionDetails.getSubstitutionCode())) {
                result++;
            }
		}

		return result;
	}

	private boolean containsCourseSubstitution(List<MapStatusReportSubstitutionDetails> reportSubstitutionDetails) {
		for (MapStatusReportSubstitutionDetails mapStatusReportSubstitutionDetails : reportSubstitutionDetails) {
			if (SubstitutionCode.SUBSTITUTABLE_COURSE.equals(mapStatusReportSubstitutionDetails.getSubstitutionCode())) {
                return true;
            }
		}

		return false;
	}

	private BigDecimal calculatePlanRatio(MapStatusReport report, List<MapPlanStatusReportCourse> planCourses,
                                          List<MapStatusReportCourseDetails> reportCourseDetails,
                                          List<MapStatusReportSubstitutionDetails> reportSubstitutionDetails,
                                          List<MapStatusReportOverrideDetails> reportOverrideDetails,
                                          boolean termBound,boolean useSubstitutableCourses,
                                          Map<String, List<MapPlanStatusReportCourse>> planCoursesByTerm,
                                          List<MapStatusReportTermDetails> reportTermDetails) {

        int totalPlanCoursesInScope = 0;

		//Calculate total number of plans in scope
		for (MapStatusReportTermDetails reportTermDetail : reportTermDetails) {
			List<MapPlanStatusReportCourse> planCoursesForTerm = planCoursesByTerm.get(reportTermDetail.getTermCode());
			if (planCoursesForTerm != null && !planCoursesForTerm.isEmpty()) {
				totalPlanCoursesInScope = totalPlanCoursesInScope + planCoursesForTerm.size();
			}
		}
		
		if (planCourses.isEmpty() || totalPlanCoursesInScope == 0) {
            return new BigDecimal(1);
        }

        int anomalies = reportCourseDetails.size();
		if (termBound == true && useSubstitutableCourses == true) {
			if (containsTermSubstitution(reportSubstitutionDetails)) {
                anomalies = anomalies + numTermSubstitution(reportSubstitutionDetails);
            }
		}

        if (termBound == false && useSubstitutableCourses == false) {
			if (containsCourseSubstitution(reportSubstitutionDetails)) {
                anomalies = anomalies + numCourseSubstitution(reportSubstitutionDetails);
            }
		}

        if (termBound == true && useSubstitutableCourses == false) {
		    if (containsTermSubstitution(reportSubstitutionDetails)) {
                anomalies = anomalies + numTermSubstitution(reportSubstitutionDetails);
            }
			if (containsCourseSubstitution(reportSubstitutionDetails)) {
                anomalies = anomalies + numCourseSubstitution(reportSubstitutionDetails);
            }
		}

//        anomalies = anomalies + reportOverrideDetails.size();

		report.setPlanRatioDemerits(anomalies);
		report.setTotalPlanCourses(totalPlanCoursesInScope);

        return new BigDecimal(new Float(totalPlanCoursesInScope - anomalies) / new Float(totalPlanCoursesInScope));
	}

	private MapStatusReport initReport(MapStatusReportPerson planIdPersonIdPair) {
		final MapStatusReport report = new MapStatusReport();
		report.setPerson(new Person(planIdPersonIdPair.getPersonId()));
		report.setPlan(new Plan(planIdPersonIdPair.getPlanId()));

        return report;
	}
	

	private String generatePlanNote(MapStatusReport report) {
		return ((report.getCourseDetails().size() > 0) ? report.getCourseDetails().size() + " plan course(s) have issues" : " ");
	}

	private void buildTermDetails(final MapStatusReport report, List<MapStatusReportTermDetails> reportTermDetails,
                                  Map<String, List<MapStatusReportCourseDetails>> courseReportsByTerm,
                                  Map<String, List<MapPlanStatusReportCourse>> planCoursesByTerm) {

		Set<String> evaluatedTerms = courseReportsByTerm.keySet();
		for (String termCode : evaluatedTerms) {
			List<MapStatusReportCourseDetails> coursesDetailsForTerm = courseReportsByTerm.get(termCode);
			//if no course details exist for a given term it means it was onplan
			MapStatusReportTermDetails termDetail = new MapStatusReportTermDetails();
			termDetail.setTermStatus(TermStatus.TEST);

            if (coursesDetailsForTerm.isEmpty()) {
				termDetail.setReport(report);
				termDetail.setTermCode(termCode);
				termDetail.setAnomalyNote("Term has no anomalies");
				termDetail.setAnomalyCode(AnomalyCode.NO_ANOMALY);
				termDetail.setTermRatio(new BigDecimal(1));

            } else {

                if (coursesDetailsForTerm.size() == 1) {
					MapStatusReportCourseDetails mapStatusReportCourseDetails = coursesDetailsForTerm.get(0);
					termDetail.setReport(report);
					termDetail.setTermCode(termCode);
					termDetail.setAnomalyNote("Term has one anomaly: "+mapStatusReportCourseDetails.getAnomalyCode().getDisplayText());
					termDetail.setAnomalyCode(mapStatusReportCourseDetails.getAnomalyCode());
					termDetail.setTermRatio(calculateTermRatio(termCode,planCoursesByTerm,coursesDetailsForTerm));
				}

                if (coursesDetailsForTerm.size() > 1) {
					termDetail.setReport(report);
					termDetail.setTermCode(termCode);
					termDetail.setAnomalyNote("There are "+coursesDetailsForTerm.size()+" anomalies for this term");
					termDetail.setAnomalyCode(AnomalyCode.MULTIPLE_ANOMALIES_IN_TERM);
					termDetail.setTermRatio(calculateTermRatio(termCode,planCoursesByTerm,coursesDetailsForTerm));
				}
			}

            reportTermDetails.add(termDetail);
		}
	}

	private BigDecimal calculateTermRatio(String termCode, Map<String, List<MapPlanStatusReportCourse>> planCoursesByTerm,
                                          List<MapStatusReportCourseDetails> coursesDetailsForTerm) {
		if (planCoursesByTerm.get(termCode).isEmpty()) {
            return new BigDecimal(1);
        }

        return new BigDecimal(new Float(planCoursesByTerm.get(termCode).size() - coursesDetailsForTerm.size())/new Float(planCoursesByTerm.get(termCode).size()));
	}

	private void evaluateTerm(Set<String> gradesSet, Set<String> criteriaSet, MapStatusReport report,
                              List<MapStatusReportCourseDetails> reportCourseDetails,
                              Map<String, List<ExternalStudentTranscriptCourse>> transcriptCoursesByTerm, Term term,
                              List<MapPlanStatusReportCourse> planCoursesForTerm,
                              Map<String, List<MapStatusReportCourseDetails>> courseReportsByTerm,
                              List<ExternalStudentTranscriptCourse> transcript,
                              Collection<ExternalSubstitutableCourse> allSubstitutableCourses,
                              List<MapStatusReportSubstitutionDetails> reportSubstitutionDetails,
                              Collection<ExternalStudentTranscriptNonCourseEntity> allNonCourseEntities,
                              List<MapStatusReportOverrideDetails> reportOverrideDetails,
                              MapStatusReportPerson planAndPersonInfo) {
		
		List<ExternalStudentTranscriptCourse> transcriptCoursesForTerm = transcriptCoursesByTerm.get(term.getCode());

		//Iterate through the courses for the term and try to find a match
		for (MapPlanStatusReportCourse mapPlanStatusReportCourse : planCoursesForTerm) {
			//Try to find term bound match
			ExternalStudentTranscriptCourse matchedTranscriptCourse = findTranscriptCourseMatch(mapPlanStatusReportCourse,transcriptCoursesForTerm,criteriaSet);

            ExternalStudentTranscriptNonCourseEntity matchedNonCourseOverride = null;
            if (matchedTranscriptCourse == null) {

                //First if no term match found check for override
                matchedNonCourseOverride = findCourseMatchOverrideCourse(mapPlanStatusReportCourse.getFormattedCourse(), mapPlanStatusReportCourse.getTermCode(), allNonCourseEntities);
                if (matchedNonCourseOverride != null) {
                    //TODO? matchedTranscriptCourse == matchedNonCourseOverride?
					reportOverrideDetails.add(createOverrideEntry(matchedNonCourseOverride, mapPlanStatusReportCourse.getTermCode(), report));
                } else {

					//Second try to find term unbounded match
					matchedTranscriptCourse = findTranscriptCourseMatch(mapPlanStatusReportCourse, transcript, criteriaSet);
					if (matchedTranscriptCourse != null) {
						//If we find a transcript match, it must have a passing grade before we log it
						if (gradesSet.contains(matchedTranscriptCourse.getGrade().trim())) {
							//If we find a term unbounded match, create an substitution entry
							reportSubstitutionDetails.add(createSubstitutionEntry(matchedTranscriptCourse, mapPlanStatusReportCourse, SubstitutionCode.TERM, report));
						}
					}
				}
			}

			if (matchedTranscriptCourse == null && matchedNonCourseOverride == null) {
				//Third try to find a substitutable course
				matchedTranscriptCourse = findTranscriptCourseMatchSubstitutableCourse(mapPlanStatusReportCourse, transcript, criteriaSet, allSubstitutableCourses, planAndPersonInfo);
				if (matchedTranscriptCourse != null) {
					//If we find a transcript match, it must have a passing grade before we log it
					if (gradesSet.contains(matchedTranscriptCourse.getGrade().trim())) {
						reportSubstitutionDetails.add(createSubstitutionEntry(matchedTranscriptCourse,mapPlanStatusReportCourse,SubstitutionCode.SUBSTITUTABLE_COURSE,report));
					} else {
					    //check if there is an override for the substitutable course
                        matchedNonCourseOverride = findCourseMatchOverrideCourse(matchedTranscriptCourse.getFormattedCourse(), matchedTranscriptCourse.getTermCode(), allNonCourseEntities);
                        if (matchedNonCourseOverride != null) {
                            reportOverrideDetails.add(createOverrideEntry(matchedNonCourseOverride, matchedTranscriptCourse.getTermCode(), report));
                        }
                    }
				} else if (mapPlanStatusReportCourse.getOriginalFormattedCourse()!=null) {
					matchedTranscriptCourse = findTranscriptCourseMatchElectiveCourse(planAndPersonInfo, mapPlanStatusReportCourse, transcript, criteriaSet);
					if (matchedTranscriptCourse != null) {
						if (gradesSet.contains(matchedTranscriptCourse.getGrade().trim())) {
							reportSubstitutionDetails.add(createSubstitutionEntry(matchedTranscriptCourse, mapPlanStatusReportCourse, SubstitutionCode.ELECTIVE_COURSE, report));
                        } else {
                            //check if there is an override for the elective course
                            matchedNonCourseOverride = findCourseMatchOverrideCourse(matchedTranscriptCourse.getFormattedCourse(), matchedTranscriptCourse.getTermCode(), allNonCourseEntities);
                            if (matchedNonCourseOverride != null) {
                                reportOverrideDetails.add(createOverrideEntry(matchedNonCourseOverride, matchedTranscriptCourse.getTermCode(), report));
                            }
                        }
					}
				}
			}

			MapStatusReportCourseDetails courseAnomaly = evaluateCourse(gradesSet, report, reportCourseDetails, term, mapPlanStatusReportCourse, matchedTranscriptCourse, matchedNonCourseOverride, criteriaSet, transcript);

            if (courseAnomaly != null) {
				courseReportsByTerm.get(term.getCode()).add(courseAnomaly);
			}
		}
	}

	private MapStatusReportSubstitutionDetails createSubstitutionEntry(
            ExternalStudentTranscriptCourse matchedTranscriptCourse,
			MapPlanStatusReportCourse mapPlanStatusReportCourse,
			SubstitutionCode substitutionCode,
			MapStatusReport report) {

        MapStatusReportSubstitutionDetails detail = new MapStatusReportSubstitutionDetails();
		detail.setFormattedCourse(mapPlanStatusReportCourse.getFormattedCourse());
		detail.setCourseCode(mapPlanStatusReportCourse.getCourseCode() == null ? " " : mapPlanStatusReportCourse.getCourseCode());
		detail.setTermCode(mapPlanStatusReportCourse.getTermCode());
		detail.setSubstitutedFormattedCourse(matchedTranscriptCourse.getFormattedCourse());
		detail.setSubstitutedTermCode(matchedTranscriptCourse.getTermCode());
		detail.setSubstitutedCourseCode(matchedTranscriptCourse.getCourseCode());
		detail.setSubstitutionNote(" ");
		detail.setSubstitutionCode(substitutionCode);
		detail.setReport(report);

        return detail;
	}

	private MapStatusReportOverrideDetails createOverrideEntry(
			ExternalStudentTranscriptNonCourseEntity matchedTranscriptNonCourse,
			String termCode,
			MapStatusReport report) {

		MapStatusReportOverrideDetails detail = new MapStatusReportOverrideDetails();
		detail.setNonCourseCode(matchedTranscriptNonCourse.getNonCourseCode());
		detail.setTermCode(termCode);
		detail.setTargetFormattedCourse(matchedTranscriptNonCourse.getTargetFormattedCourse());
		detail.setDescription(matchedTranscriptNonCourse.getDescription());
		detail.setOverrideNote(" ");
		detail.setReport(report);

		return detail;
	}

	private ExternalStudentTranscriptCourse findTranscriptCourseMatchSubstitutableCourse(
			MapPlanStatusReportCourse mapPlanStatusReportCourse,
			List<ExternalStudentTranscriptCourse> transcript,
			Set<String> criteriaSet, Collection<ExternalSubstitutableCourse> allSubstitutableCourses,
            MapStatusReportPerson planAndPersonInfo) {

        if (allSubstitutableCourses == null || allSubstitutableCourses.isEmpty()) {
            return null;
        }

        for (ExternalSubstitutableCourse substitutableCourse : allSubstitutableCourses) {
			//If term or program code are defined as null as part of the substitution then it's considered term or program unbounded
			//In otherwords, if term is null it applies to all terms, if program is null is applies to all programs
			if ((mapPlanStatusReportCourse.getFormattedCourse().trim().equalsIgnoreCase(substitutableCourse.getSourceFormattedCourse().trim()) &&
				(!criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_COURSE_TITLE) || mapPlanStatusReportCourse.getCourseTitle().trim().equalsIgnoreCase(substitutableCourse.getSourceCourseTitle())) &&
				(!criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_CREDIT_HOURS) || mapPlanStatusReportCourse.getCreditHours().equals(substitutableCourse.getSourceCreditHours())) &&
				(!criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_COURSE_CODE) || mapPlanStatusReportCourse.getCourseCode().trim().equals(substitutableCourse.getSourceCourseCode()))) 
			   && (substitutableCourse.getTermCode() == null || (mapPlanStatusReportCourse.getTermCode() != null && mapPlanStatusReportCourse.getTermCode().trim().equalsIgnoreCase(substitutableCourse.getTermCode().trim())))
		       && (substitutableCourse.getProgramCode() == null || (planAndPersonInfo.getProgramCode() != null && planAndPersonInfo.getProgramCode().trim().equalsIgnoreCase(substitutableCourse.getProgramCode().trim())))					
			   && (substitutableCourse.getCatalogYearCode() == null || (planAndPersonInfo.getCatalogYearCode() != null &&  planAndPersonInfo.getCatalogYearCode().trim().equalsIgnoreCase(substitutableCourse.getCatalogYearCode().trim())))					)
				
			{
				//if a substitution is found, check to see if the student has taken the target course
				for (ExternalStudentTranscriptCourse transcriptCourse : transcript) {
					if( (transcriptCourse.getFormattedCourse().trim().equalsIgnoreCase(substitutableCourse.getTargetFormattedCourse().trim()) &&
							(!criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_COURSE_TITLE) || transcriptCourse.getTitle().trim().equalsIgnoreCase(substitutableCourse.getTargetCourseTitle())) &&
							(!criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_CREDIT_HOURS) || transcriptCourse.getCreditEarned().equals(substitutableCourse.getTargetCreditHours())) &&
							(!criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_COURSE_CODE) || transcriptCourse.getCourseCode().trim().equals(substitutableCourse.getTargetCourseCode()))) 
							&& (substitutableCourse.getTermCode() == null || transcriptCourse.getTermCode().trim().equalsIgnoreCase(substitutableCourse.getTermCode().trim()))
							&& (substitutableCourse.getProgramCode() == null || planAndPersonInfo.getProgramCode().trim().equalsIgnoreCase(substitutableCourse.getProgramCode().trim())
							&& (substitutableCourse.getCatalogYearCode() == null || planAndPersonInfo.getCatalogYearCode().trim().equalsIgnoreCase(substitutableCourse.getCatalogYearCode().trim())))
							) {
                        return transcriptCourse;
                    }
				}
			}			
		}

		return null;
	}
	private ExternalStudentTranscriptCourse findTranscriptCourseMatchElectiveCourse(
			MapStatusReportPerson planAndPersonInfo,
			MapPlanStatusReportCourse mapPlanStatusReportCourse,
			List<ExternalStudentTranscriptCourse> transcript,
			Set<String> criteriaSet) {

		Plan plan;
		try {
			plan = planService.get(planAndPersonInfo.getPlanId());
		} catch (ObjectNotFoundException e) {
			return null;
		}
		PlanElectiveCourse planElectiveCourse = getPlanElectiveCourse(plan, mapPlanStatusReportCourse.getOriginalFormattedCourse());
		if (planElectiveCourse==null){
			return null;
		}

		ExternalStudentTranscriptCourse transcriptCourse = findExternalStudentTranscriptCourseForElectiveCourse(transcript, criteriaSet, planElectiveCourse.getFormattedCourse(),
				planElectiveCourse.getCourseTitle(), planElectiveCourse.getCreditHours(), planElectiveCourse.getCourseCode());
		if (transcriptCourse != null) {
			return transcriptCourse;
		}
		for (AbstractMapElectiveCourse planElectiveCourseElective : planElectiveCourse.getElectiveCourseElectives()) {
			transcriptCourse = findExternalStudentTranscriptCourseForElectiveCourse(transcript, criteriaSet, planElectiveCourseElective.getFormattedCourse(),
					planElectiveCourseElective.getCourseTitle(), planElectiveCourseElective.getCreditHours(), planElectiveCourseElective.getCourseCode());
			if (transcriptCourse != null) {
				return transcriptCourse;
			}
		}
		return null;
	}

	private ExternalStudentTranscriptCourse findExternalStudentTranscriptCourseForElectiveCourse(List<ExternalStudentTranscriptCourse> transcript, Set<String> criteriaSet,
																								 String formattedCourse, String courseTitle, BigDecimal creditHours, String courseCode){
		for (ExternalStudentTranscriptCourse transcriptCourse : transcript) {
			if( (transcriptCourse.getFormattedCourse().trim().equalsIgnoreCase(formattedCourse.trim()) &&
					(!criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_COURSE_TITLE) || transcriptCourse.getTitle().trim().equalsIgnoreCase(courseTitle)) &&
					(!criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_CREDIT_HOURS) || transcriptCourse.getCreditEarned().equals(creditHours)) &&
					(!criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_COURSE_CODE) || transcriptCourse.getCourseCode().trim().equals(courseCode)))
					)
				return transcriptCourse;
		}
		return null;
	}

	private MapStatusReportCourseDetails evaluateCourse(Set<String> passingGradesSet,
			MapStatusReport report,
			List<MapStatusReportCourseDetails> reportCourseDetails, Term term,
			MapPlanStatusReportCourse mapPlanStatusReportCourse,
			ExternalStudentTranscriptCourse matchedTranscriptCourse,
            ExternalStudentTranscriptNonCourseEntity nonCourseEntityOverride,
			Set<String> criteriaSet, List<ExternalStudentTranscriptCourse> transcript) {
		
		MapStatusReportCourseDetails courseDetail = null;

		if (nonCourseEntityOverride != null) {
            return courseDetail; //no anomaly since the override will override an existing course

        } else if (matchedTranscriptCourse == null && nonCourseEntityOverride == null) {

            //If there is no matched course and no override, that's an anomaly
            if ( term.getEndDate().after(new Date()) ) {
                courseDetail = new MapStatusReportCourseDetails(report, mapPlanStatusReportCourse.getTermCode(), mapPlanStatusReportCourse.getFormattedCourse(),
                        mapPlanStatusReportCourse.getCourseCode(), "", AnomalyCode.COURSE_NOT_REGISTERED);
                reportCourseDetails.add(courseDetail);
            } else {
                courseDetail = new MapStatusReportCourseDetails(report, mapPlanStatusReportCourse.getTermCode(), mapPlanStatusReportCourse.getFormattedCourse(),
                        mapPlanStatusReportCourse.getCourseCode(), "", AnomalyCode.COURSE_NOT_TAKEN);
                reportCourseDetails.add(courseDetail);
            }

        } else {

            //If the grade in the transcript is not a configured 'passing grade', that's an anomaly
            if ( !passingGradesSet.contains(matchedTranscriptCourse.getGrade().trim().toUpperCase()) ) {
                if ( term.getEndDate().after(new Date()) ) {
                    courseDetail = new MapStatusReportCourseDetails(report, mapPlanStatusReportCourse.getTermCode(), mapPlanStatusReportCourse.getFormattedCourse(),
                            mapPlanStatusReportCourse.getCourseCode(), "", AnomalyCode.CURR_OR_FUT_COURSE_NO_GRADE);
                    reportCourseDetails.add(courseDetail);

                } else {
                	//SSP-3158 The matched course has a failing grade.  Look again for the course for a passingGrade and if not found (NULL) then add the anomaly
					if (configService.getByNameOrDefaultValue(CONFIGURABLE_MATCH_COURSE_ANY_PASSING_COURSE)) {
						if (findTranscriptCourseMatch(mapPlanStatusReportCourse, transcript, criteriaSet, passingGradesSet) == null) {
							courseDetail = new MapStatusReportCourseDetails(report, mapPlanStatusReportCourse.getTermCode(), mapPlanStatusReportCourse.getFormattedCourse(),
									mapPlanStatusReportCourse.getCourseCode(), "", AnomalyCode.COURSE_NOT_PASSED);
							reportCourseDetails.add(courseDetail);
						}
					} else {
						if (findLastTranscriptCourseMatch(mapPlanStatusReportCourse, transcript, criteriaSet, passingGradesSet) == null) {
							courseDetail = new MapStatusReportCourseDetails(report, mapPlanStatusReportCourse.getTermCode(), mapPlanStatusReportCourse.getFormattedCourse(),
									mapPlanStatusReportCourse.getCourseCode(), "", AnomalyCode.COURSE_NOT_PASSED);
							reportCourseDetails.add(courseDetail);
						}
					}
                }
            }
        }

		//will be null if no anomaly is found
		return courseDetail;
	}
	private ExternalStudentTranscriptCourse findLastTranscriptCourseMatch(MapPlanStatusReportCourse mapPlanStatusReportCourse,
																	  List<ExternalStudentTranscriptCourse> transcriptCoursesForTerm, Set<String> criteriaSet, Set<String> passingGradesSet) {
		if (transcriptCoursesForTerm == null || transcriptCoursesForTerm.isEmpty()) {
			return null;
		}

		ExternalStudentTranscriptCourse lastExternalStudentTranscriptCourse = null;
		for (ExternalStudentTranscriptCourse externalStudentTranscriptCourse : transcriptCoursesForTerm) {
			if (isCourseMatch(mapPlanStatusReportCourse, externalStudentTranscriptCourse, criteriaSet)) {
				if (lastExternalStudentTranscriptCourse==null) {
					lastExternalStudentTranscriptCourse = externalStudentTranscriptCourse;
				} else {
					if (compareTerms(lastExternalStudentTranscriptCourse.getTermCode(), externalStudentTranscriptCourse.getTermCode()) == 1) {
						lastExternalStudentTranscriptCourse = externalStudentTranscriptCourse;
					}
				}
			}

		}
		if (lastExternalStudentTranscriptCourse!=null) {
			if (passingGradesSet!=null) {
				if (passingGradesSet.contains(lastExternalStudentTranscriptCourse.getGrade().trim().toUpperCase())) {
					return lastExternalStudentTranscriptCourse;
				}
			} else {
				return lastExternalStudentTranscriptCourse;
			}
		}
		return null;
	}

	private ExternalStudentTranscriptCourse findTranscriptCourseMatch(MapPlanStatusReportCourse mapPlanStatusReportCourse,
																	  List<ExternalStudentTranscriptCourse> transcriptCoursesForTerm, Set<String> criteriaSet) {
		return findTranscriptCourseMatch(mapPlanStatusReportCourse, transcriptCoursesForTerm, criteriaSet, null);
	}

	private ExternalStudentTranscriptCourse findTranscriptCourseMatch(MapPlanStatusReportCourse mapPlanStatusReportCourse,
                              List<ExternalStudentTranscriptCourse> transcriptCoursesForTerm, Set<String> criteriaSet, Set<String> passingGradesSet) {

        if (transcriptCoursesForTerm == null || transcriptCoursesForTerm.isEmpty()) {
            return null;
        }

		for (ExternalStudentTranscriptCourse externalStudentTranscriptCourse : transcriptCoursesForTerm) {
			boolean match = isCourseMatch(mapPlanStatusReportCourse, externalStudentTranscriptCourse, criteriaSet);

			if (passingGradesSet!=null) {
				if (!passingGradesSet.contains(externalStudentTranscriptCourse.getGrade().trim().toUpperCase())) {
					match = false;
				}
			}

            if(match) {
				return externalStudentTranscriptCourse;
			}
		}

		return null;
	}

	private boolean isCourseMatch(MapPlanStatusReportCourse mapPlanStatusReportCourse,
								  ExternalStudentTranscriptCourse externalStudentTranscriptCourse, Set<String> criteriaSet){
		boolean match = true;
		if (!mapPlanStatusReportCourse.getFormattedCourse().trim().equalsIgnoreCase(externalStudentTranscriptCourse.getFormattedCourse().trim())) {
			match = false;
		}

		//The default matching criteria is term_code + formatted_course.  There may be schools where this isn't good enough
		//so these additional search criteria are configurable in case schools need something more detailed.
		if (criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_COURSE_TITLE)) {
			if (!mapPlanStatusReportCourse.getCourseTitle().trim().equalsIgnoreCase(externalStudentTranscriptCourse.getTitle().trim())) {
				match = false;
			}
		}

		if (criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_CREDIT_HOURS)) {
			if (!mapPlanStatusReportCourse.getCreditHours().equals(externalStudentTranscriptCourse.getCreditEarned())) {
				match = false;
			}
		}

		if (criteriaSet.contains(MapStatusReportServiceImpl.CONFIGURABLE_MATCH_CRITERIA_COURSE_CODE)) {
			//its quite possible that course code may not be on the transcript.  If this is true, we shouldn't consider course_code at all
			if (mapPlanStatusReportCourse.getCourseCode() != null && externalStudentTranscriptCourse.getCourseCode() != null) {
				if (!mapPlanStatusReportCourse.getCourseCode().trim().equalsIgnoreCase(externalStudentTranscriptCourse.getCourseCode().trim())) {
					match = false;
				}
			}
		}

		return match;
	}

	private int compareTerms(String firstTermCode, String secondTermCode) {
		Date firstTermEndDate = null;
		Date secondTermEndDate = null;
		try {
			firstTermEndDate = termService.getByCode(firstTermCode).getEndDate();
		} catch (ObjectNotFoundException e) {}
		try {
			secondTermEndDate = termService.getByCode(secondTermCode).getEndDate();
		} catch (ObjectNotFoundException e) {}

		if (firstTermEndDate!=null && secondTermEndDate!=null ) {
			return secondTermEndDate.compareTo(firstTermEndDate);
		}
		if (secondTermEndDate!=null) {
			return 1;
		}
		return -1;
	}

    private ExternalStudentTranscriptNonCourseEntity findCourseMatchOverrideCourse(String formattedCourse, String termCode,
                                                                      Collection<ExternalStudentTranscriptNonCourseEntity> allNonCourseEntities) {

        if (allNonCourseEntities == null || allNonCourseEntities.isEmpty()) {
            return null;
        }

        for (ExternalStudentTranscriptNonCourseEntity externalStudentTranscriptNonCourseEntity : allNonCourseEntities) {

            if (formattedCourse.trim().equalsIgnoreCase(externalStudentTranscriptNonCourseEntity.getTargetFormattedCourse().trim())
                     && termCode.trim().equalsIgnoreCase(externalStudentTranscriptNonCourseEntity.getTermCode().trim())) {
                return externalStudentTranscriptNonCourseEntity;
            }
        }

        return null;
    }

	private Map<String, List<ExternalStudentTranscriptCourse>> organizeTranscriptCourseByTerm(List<ExternalStudentTranscriptCourse> transcriptsBySchoolId) {
		Map<String, List<ExternalStudentTranscriptCourse>> transcriptCoursesByTerm = new HashMap<String,List<ExternalStudentTranscriptCourse>>();

        for (ExternalStudentTranscriptCourse transcriptCourse:transcriptsBySchoolId) {

            if (transcriptCoursesByTerm.get(transcriptCourse.getTermCode()) == null) {
				List<ExternalStudentTranscriptCourse> list = new ArrayList<ExternalStudentTranscriptCourse>();
				list.add(transcriptCourse);
				transcriptCoursesByTerm.put(transcriptCourse.getTermCode(), list);
			} else {
				List<ExternalStudentTranscriptCourse> list = transcriptCoursesByTerm.get(transcriptCourse.getTermCode());
				list.add(transcriptCourse);
			}
		}

		return transcriptCoursesByTerm;
	}

	private Map<String, List<MapPlanStatusReportCourse>> organizePlanCoursesByTerm(List<MapPlanStatusReportCourse> planCourses) {
		Map<String, List<MapPlanStatusReportCourse>> planCoursesByTerm = new HashMap<String,List<MapPlanStatusReportCourse>>();

        for (MapPlanStatusReportCourse planCourse:planCourses) {

            if (planCoursesByTerm.get(planCourse.getTermCode()) == null) {
				List<MapPlanStatusReportCourse> list = new ArrayList<MapPlanStatusReportCourse>();
				list.add(planCourse);
				planCoursesByTerm.put(planCourse.getTermCode(), list);
			} else {
				List<MapPlanStatusReportCourse> list = planCoursesByTerm.get(planCourse.getTermCode());
				list.add(planCourse);
			}
		}

        return planCoursesByTerm;
	}
  

	@Override
	public List<ExternalSubstitutableCourse> getAllSubstitutableCourses() {
		return externalSubstitutableCourseDao.getAllSubstitutableCourses();
	}
	
	@Override
	public Set<String> getAdditionalCriteria() {
		Set<String> additionalCriteriaSet = new HashSet<String>();
		String additionalMatchingCriteriaString = configService.getByNameEmpty("map_plan_status_addition_course_matching_criteria");

        if (!StringUtils.isEmpty(additionalMatchingCriteriaString.trim())) {
			String[] criteria = additionalMatchingCriteriaString.split(",");
			for (String string : criteria) {
				//Normalize additional criteria to upper/trimmed 
				additionalCriteriaSet.add(string.toUpperCase().trim());
			}
		}

        return additionalCriteriaSet;
	}

	@Override
	public Set<String> getPassingGrades() {
		Set<String> gradesSet = new HashSet<String>();
		String[] grades = configService.getByNameEmpty("map_plan_status_passing_grades").split(",");

        for (String string : grades) {
			//Normalize grades to upper/trimmed 
			gradesSet.add(string.toUpperCase().trim());
		}

        return gradesSet;
	}

	@Override
	public Term deriveCutoffTerm() {
		final String cutoffTermCode = configService.getByNameEmpty("map_plan_status_cutoff_term_code");

        if (StringUtils.isNotBlank(cutoffTermCode)) {
            try {
                return termService.getByCode(cutoffTermCode.trim());
            } catch (ObjectNotFoundException e) {
                LOGGER.error("Map Status Calculation can't find Cutoff Term {}. Check the configuration or EXTERNAL_TERM table for accuracy.", cutoffTermCode.trim());
            }
        }

        //If a registration window is open for a defined term.  Use that
        final List<Term> termsWithRegistrationWindowOpen = termService.getTermsWithRegistrationWindowOpenIfAny();
        Term latestTerm = null;

        if (CollectionUtils.isNotEmpty(termsWithRegistrationWindowOpen)) {
            for (Term term : termsWithRegistrationWindowOpen) {
                if (latestTerm == null) {
                    latestTerm = term;
                } else {
                    if (term.getStartDate().after(latestTerm.getStartDate())) {
                        latestTerm = term;
                    }
                }
            }
        }

        try {
            //If there is no registration window open, go with the current term.
            return ((latestTerm == null) ? termService.getCurrentTerm() : latestTerm);
        } catch (ObjectNotFoundException e) {
            //if we can't resolve the current term, we have bigger problems
            LOGGER.error("Map Status Calculation will stop because the current term cannot be resolved.  This is likely a data issue in the EXTERNAL_TERM table");
            throw new IllegalArgumentException("Current term could not be resolved.  Map Report Calculation aborted.");
        }
	}

	@Override
	public List<MapStatusReportSummaryDetail> getSummaryDetails() {
		return dao.getSummaryDetails();
	}

	@Override
	public List<MapStatusReportOwnerAndCoachInfo> getOwnersAndCoachesWithOffPlanStudent() {
		return dao.getOwnersAndCoachesWithOffPlanStudent();
	}

	@Override
	public List<MapStatusReportPerson> getOffPlanPlansForOwner(Person owner) {
		return dao.getOffPlanPlansForOwner(owner);
	}

	@Override
	public List<MapStatusReportCourseDetails> getAllCourseDetailsForPerson(
			Person person) {
		return dao.getAllCourseDetailsForPerson(person);
	}

	@Override
	public List<MapStatusReportTermDetails> getAllTermDetailsForPerson(
			Person person) {
		return dao.getAllTermDetailsForPerson(person);

	}

	@Override
	public List<MapStatusReportSubstitutionDetails> getAllSubstitutionDetailsForPerson(
			Person person) {
		return dao.getAllSubstitutionDetailsForPerson(person);

	}

	@Override
	public List<MapStatusReportOverrideDetails> getAllOverrideDetailsForPerson(
			Person person) {
		return dao.getAllOverrideDetailsForPerson(person);

	}

	@Override
	public void deleteAllOldReportsForStudent(UUID personId) {
		 dao.deleteAllOldReportsForPerson(personId);
	}

	@Override
	public Boolean calculateStatusForStudent(UUID personId) throws ObjectNotFoundException, ValidationException {

        deleteAllOldReportsForStudent(personId);

		//Load up our configs
        Set<String> gradesSet = getPassingGrades();
		Set<String> additionalCriteriaSet = getAdditionalCriteria();
		boolean termBound = Boolean.parseBoolean(configService.getByNameEmpty("map_plan_status_term_bound_strict").trim());
		boolean useSubstitutableCourses = Boolean.parseBoolean(configService.getByNameEmpty("map_plan_status_use_substitutable_courses").trim());

		//Lets figure out our cutoff term
		Term cutoffTerm = deriveCutoffTerm();
		
		//Lightweight query to avoid the potential 'kitchen sink' we would pull out if we fetched the Plan object
		Plan plan = planService.getCurrentForStudent(personId);

        //If there is no active plan return false
		if (plan == null) {
            return false;
        }

        final String studentSchoolId = plan.getPerson().getSchoolId();
		
		List<ExternalStudentTranscriptCourse> transcript = externalStudentTranscriptCourseService.getTranscriptsBySchoolId(studentSchoolId);
		
		Collection<ExternalSubstitutableCourse> allSubstitutableCourses =
				useSubstitutableCourses ? getAllPossibleSubstitutableCoursesForStudent(plan,transcript) : Lists.<ExternalSubstitutableCourse>newArrayList();

        Collection<ExternalStudentTranscriptNonCourseEntity> allNonCourseEntities = externalStudentTranscriptNonCourseEntityService.getNonCourseTranscriptsBySchoolId(studentSchoolId);
		
		MapStatusReportPerson mapStatusReportPerson = new MapStatusReportPerson(plan.getId(), personId, plan.getPerson().getSchoolId(), plan.getProgramCode(),plan.getCatalogYearCode(), plan.getPerson().getFirstName(), plan.getPerson().getLastName(), plan.getPerson().getCoach().getId(), plan.getOwner().getId());
		List<Term> allTerms = termService.getAll();
		
		MapStatusReport report = evaluatePlan(gradesSet, additionalCriteriaSet, cutoffTerm, allTerms,
                mapStatusReportPerson, allSubstitutableCourses, allNonCourseEntities, transcript,
                termBound, useSubstitutableCourses);
		save(report);
		
		return true;
	}

	private Collection<ExternalSubstitutableCourse> getAllPossibleSubstitutableCoursesForStudent(
			Plan plan, List<ExternalStudentTranscriptCourse> transcript) {
		return externalSubstitutableCourseDao.getAllPossibleSubstitutableCoursesForStudent(plan, transcript);
	}

	@Override
	public List<MapStatusReportOwnerAndCoachInfo> getWatchersOffPlanStudent() {
		return dao.getWatchersOffPlanStudent();
	}

	@Override
	public List<MapStatusReportPerson> getOffPlanPlansForWatcher(Person person) {
		return dao.getOffPlanPlansForWatcher(person);
	}

	private PlanElectiveCourse getPlanElectiveCourse(Plan plan, String originalFormattedCourse) {
		for (PlanElectiveCourse planElectiveCourse : plan.getPlanElectiveCourses()) {
			if (planElectiveCourse.getFormattedCourse().equals(originalFormattedCourse)) {
				return planElectiveCourse;
			}
		}
		return null;
	}
}
