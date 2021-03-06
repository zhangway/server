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
package org.ohmage.request.visualization;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.ohmage.annotator.Annotator.ErrorCode;
import org.ohmage.exception.InvalidRequestException;
import org.ohmage.exception.ServiceException;
import org.ohmage.exception.ValidationException;
import org.ohmage.request.InputKeys;
import org.ohmage.service.CampaignServices;
import org.ohmage.service.VisualizationServices;
import org.ohmage.validator.CampaignValidators;

/**
 * <p>A request for a 2D density plot of two prompt types in the same campaign.
 * <br />
 * <br />
 * See {@link org.ohmage.request.visualization.VisualizationRequest} for other 
 * required parameters.</p>
 * <table border="1">
 *   <tr>
 *     <td>Parameter Name</td>
 *     <td>Description</td>
 *     <td>Required</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#PROMPT_ID}</td>
 *     <td>The first prompt ID in the campaign's XML.</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#PROMPT2_ID}</td>
 *     <td>The second prompt ID in the campaign's XML.</td>
 *     <td>true</td>
 *   </tr>
 * </table>
 * 
 * @author John Jenkins
 */
public class VizTwoDDensityRequest extends VisualizationRequest {
	private static final Logger LOGGER = Logger.getLogger(VizTwoDDensityRequest.class);
	
	private static final String REQUEST_PATH = "biplot/png";
	
	private final String promptId;
	private final String prompt2Id;
	
	/**
	 * Creates a 2D density visualization request.
	 * 
	 * @param httpRequest The HttpServletRequest with the required parameters.
	 * 
	 * @throws InvalidRequestException Thrown if the parameters cannot be 
	 * 								   parsed.
	 * 
	 * @throws IOException There was an error reading from the request.
	 */
	public VizTwoDDensityRequest(HttpServletRequest httpRequest) throws IOException, InvalidRequestException {
		super(httpRequest);
		
		LOGGER.info("Creating a 2D density request.");
		
		String tPromptId = null;
		String tPrompt2Id = null;
		
		try {
			tPromptId = CampaignValidators.validatePromptId(httpRequest.getParameter(InputKeys.PROMPT_ID));
			if(tPromptId == null) {
				setFailed(ErrorCode.SURVEY_INVALID_PROMPT_ID, "Missing the parameter: " + InputKeys.PROMPT_ID);
				throw new ValidationException("Missing the parameter: " + InputKeys.PROMPT_ID);
			}
			
			tPrompt2Id = CampaignValidators.validatePromptId(httpRequest.getParameter(InputKeys.PROMPT2_ID));
			if(tPrompt2Id == null) {
				setFailed(ErrorCode.SURVEY_INVALID_PROMPT_ID, "Missing the parameter: " + InputKeys.PROMPT2_ID);
				throw new ValidationException("Missing the parameter: " + InputKeys.PROMPT2_ID);
			}
		}
		catch(ValidationException e) {
			e.failRequest(this);
			LOGGER.info(e.toString());
		}
		
		promptId = tPromptId;
		prompt2Id = tPrompt2Id;
	}
	
	/**
	 * Services the request.
	 */
	@Override
	public void service() {
		LOGGER.info("Servicing the 2D density visualization request.");
		
		if(! authenticate(AllowNewAccount.NEW_ACCOUNT_DISALLOWED)) {
			return;
		}
		
		super.service();
		
		if(isFailed()) {
			return;
		}
		
		try {
			LOGGER.info("Verifying that the first prompt ID exists in the campaign's XML");
			CampaignServices.instance().ensurePromptExistsInCampaign(getCampaignId(), promptId);
			
			LOGGER.info("Verifying that the second prompt ID exists in the campaign's XML");
			CampaignServices.instance().ensurePromptExistsInCampaign(getCampaignId(), prompt2Id);
			
			Map<String, String> parameters = getVisualizationParameters();
			parameters.put(VisualizationServices.PARAMETER_KEY_PROMPT_ID, promptId);
			parameters.put(VisualizationServices.PARAMETER_KEY_PROMPT2_ID, prompt2Id);
			
			LOGGER.info("Making the request to the visualization server.");
			setImage(VisualizationServices.sendVisualizationRequest(REQUEST_PATH, getUser().getToken(), 
					getCampaignId(), getWidth(), getHeight(), parameters));
		}
		catch(ServiceException e) {
			e.failRequest(this);
			e.logException(LOGGER);
		}
	}
}
