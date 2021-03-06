/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.request.campaign;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.annotator.Annotator.ErrorCode;
import org.ohmage.domain.campaign.Campaign;
import org.ohmage.exception.DomainException;
import org.ohmage.exception.InvalidRequestException;
import org.ohmage.exception.ServiceException;
import org.ohmage.exception.ValidationException;
import org.ohmage.request.InputKeys;
import org.ohmage.request.UserRequest;
import org.ohmage.service.CampaignServices;
import org.ohmage.service.UserCampaignServices;
import org.ohmage.service.UserServices;
import org.ohmage.validator.CampaignValidators;

/**
 * <p>Gathers all campaigns and then searches through them removing those that 
 * do not match the search criteria. A missing or empty string for a parameter 
 * means that it will not be used to filter the list. If no parameters are 
 * given, information about every campaign in the system will be returned.</p>
 * <p>The requester must be an admin.</p>
 * <table border="1">
 *   <tr>
 *     <td>Parameter Name</td>
 *     <td>Description</td>
 *     <td>Required</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#CLIENT}</td>
 *     <td>A string describing the client that is making this request.</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#CAMPAIGN_URN}</td>
 *     <td>Part or all of a campaign ID that will limit the results to only 
 *       those campaigns that contain this value in their ID.</td>
 *     <td>false</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#CAMPAIGN_NAME}</td>
 *     <td>Part or all of a campaign name that will limit the results to only 
 *       those campaigns that contain this value in their name.</td>
 *     <td>false</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#DESCRIPTION}</td>
 *     <td>Part or all of a campaign description that will limit the results to 
 *       only those campaigns that contain this value in their description.
 *       </td>
 *     <td>false</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#XML}</td>
 *     <td>Part or all of a campaign XML that will limit the results to only 
 *       those campaigns that contain this value in their XML. Unlike the XML 
 *       in campaign creation and update, this should not be an attached file
 *       and should instead be a parameter like all of the other parameters.
 *       </td>
 *     <td>false</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#CAMPAIGN_AUTHORED_BY}</td>
 *     <td>Part or all of a campaign's authored by value that will limit the  
 *       result to only those campaigns that contain this value in their 
 *       authored by value.</td>
 *     <td>false</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#START_DATE}</td>
 *     <td>Limits the results to only those that took place on or after this
 *       date.</td>
 *     <td>false</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#END_DATE}</td>
 *     <td>Limits the results to only those that took place on or before this
 *       date.</td>
 *     <td>false</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#PRIVACY_STATE}</td>
 *     <td>Limits the results to only those that have this privacy state.</td>
 *     <td>false</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#RUNNING_STATE}</td>
 *     <td>Limits the results to only those that have this running state.</td>
 *     <td>false</td>
 *   </tr>
 * </table>
 * 
 * @author John Jenkins
 */
public class CampaignSearchRequest extends UserRequest {
	private static final Logger LOGGER = Logger.getLogger(CampaignSearchRequest.class);

	private final String campaignId;
	private final String campaignName;
	private final String description;
	private final String xml;
	private final String authoredBy;
	private final DateTime startDate;
	private final DateTime endDate;
	private final Campaign.PrivacyState privacyState;
	private final Campaign.RunningState runningState;
	
	private final Collection<Campaign> campaigns;
	
	/**
	 * Builds this request based on the information in the HTTP request.
	 * 
	 * @param httpRequest A HttpServletRequest object that contains the
	 * 					  parameters to and metadata for this request.
	 * 
	 * @throws InvalidRequestException Thrown if the parameters cannot be 
	 * 								   parsed.
	 * 
	 * @throws IOException There was an error reading from the request.
	 */
	public CampaignSearchRequest(HttpServletRequest httpRequest) throws IOException, InvalidRequestException {
		super(httpRequest, null, TokenLocation.EITHER, null);
		
		String tCampaignId = null;
		String tCampaignName = null;
		String tDescription = null;
		String tXml = null;
		String tAuthoredBy = null;
		DateTime tStartDate = null;
		DateTime tEndDate = null;
		Campaign.PrivacyState tPrivacyState = null;
		Campaign.RunningState tRunningState = null;
		
		if(! isFailed()) {
			LOGGER.info("Creating a campaign search request.");
			
			String[] t = null;
			try {
				t = getParameterValues(InputKeys.CAMPAIGN_URN);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.CAMPAIGN_INVALID_ID,
							"Multiple campaign IDs were given: " +
								InputKeys.CAMPAIGN_URN);
				}
				else if(t.length == 1) {
					tCampaignId = t[0];
				}
				
				t = getParameterValues(InputKeys.CAMPAIGN_NAME);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.CLASS_INVALID_NAME,
							"Multiple campaign names were given: " +
								InputKeys.CAMPAIGN_NAME);
				}
				else if(t.length == 1) {
					tCampaignName = t[0];
				}
				
				t = getParameterValues(InputKeys.DESCRIPTION);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.CLASS_INVALID_DESCRIPTION,
							"Multiple campaign descriptions were given: " +
								InputKeys.DESCRIPTION);
				}
				else if(t.length == 1) {
					tDescription = t[0];
				}
				
				t = getParameterValues(InputKeys.XML);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.CAMPAIGN_INVALID_XML,
							"Multiple campaign XMLs were given: " + 
								InputKeys.XML);
				}
				else if(t.length == 1) {
					tXml = t[0];
				}
				
				t = getParameterValues(InputKeys.CAMPAIGN_AUTHORED_BY);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.CAMPAIGN_INVALID_AUTHORED_BY_VALUE,
							"Multiple campaign 'authored by' values were given: " +
								InputKeys.CAMPAIGN_AUTHORED_BY);
				}
				else if(t.length == 1) {
					tAuthoredBy = t[0];
				}
				
				t = getParameterValues(InputKeys.START_DATE);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.SERVER_INVALID_DATE,
							"Multiple start dates were given: " +
								InputKeys.START_DATE);
				}
				else if(t.length == 1) {
					tStartDate = CampaignValidators.validateStartDate(t[0]);
				}
				
				t = getParameterValues(InputKeys.END_DATE);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.SERVER_INVALID_DATE,
							"Multiple end dates were given: " +
								InputKeys.END_DATE);
				}
				else if(t.length == 1) {
					tEndDate = CampaignValidators.validateEndDate(t[0]);
				}
				
				t = getParameterValues(InputKeys.PRIVACY_STATE);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.CAMPAIGN_INVALID_PRIVACY_STATE,
							"Multiple privacy states were given: " +
								InputKeys.PRIVACY_STATE);
				}
				else if(t.length == 1) {
					tPrivacyState = CampaignValidators.validatePrivacyState(t[0]);
				}
				
				t = getParameterValues(InputKeys.RUNNING_STATE);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.CAMPAIGN_INVALID_RUNNING_STATE,
							"Multiple running states were given: " +
								InputKeys.RUNNING_STATE);
				}
				else if(t.length == 1) {
					tRunningState = CampaignValidators.validateRunningState(t[0]);
				}
			}
			catch(ValidationException e) {
				e.failRequest(this);
				e.logException(LOGGER);
			}
		}
		
		campaignId = tCampaignId;
		campaignName = tCampaignName;
		description = tDescription;
		xml = tXml;
		authoredBy = tAuthoredBy;
		startDate = tStartDate;
		endDate = tEndDate;
		privacyState = tPrivacyState;
		runningState = tRunningState;
		
		campaigns = new LinkedList<Campaign>();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ohmage.request.Request#service()
	 */
	@Override
	public void service() {
		LOGGER.info("Servicing the campaign search request.");
		
		if(! authenticate(AllowNewAccount.NEW_ACCOUNT_DISALLOWED)) {
			return;
		}
		
		try {
			LOGGER.info("Checking that the user is an admin.");
			UserServices.instance().verifyUserIsAdmin(getUser().getUsername());

			LOGGER.info("Searching for the campaigns that satisfy the parameters.");
			Set<String> campaignIds =
				CampaignServices.instance().campaignIdSearch(
						campaignId,
						campaignName,
						description,
						xml,
						authoredBy,
						startDate,
						endDate,
						privacyState,
						runningState);
			
			LOGGER.info("Gathering the information about each of the campaigns.");
			campaigns.addAll(
					UserCampaignServices
						.instance()
							.getCampaignAndUserRolesForCampaigns(
									null, 
									campaignIds, 
									true
								)
								.keySet()
				);
		}
		catch(ServiceException e) {
			e.failRequest(this);
			e.logException(LOGGER);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.request.Request#respond(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void respond(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		LOGGER.info("Responding to a campaign search request.");
		JSONObject result = null;
		
		if(! isFailed()) {
			result = new JSONObject();
			
			try {
				for(Campaign campaign : campaigns) {
					result.put(
							campaign.getId(), 
							campaign.toJson(
									false, 
									true, 
									true, 
									true, 
									true, 
									true, 
									true, 
									true, 
									false
								)
						);
				}
			}
			catch(JSONException e) {
				LOGGER.error("There was an error building the result.", e);
				setFailed();
			}
			catch(DomainException e) {
				LOGGER
					.error(
						"There was an error building the campaign mask.",
						e);
				setFailed();
			}
		}
		
		super.respond(httpRequest, httpResponse, result);
	}
}
