package org.ohmage.domain.campaign.response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ohmage.domain.campaign.PromptResponse;
import org.ohmage.domain.campaign.prompt.MultiChoiceCustomPrompt;

/**
 * A response to a multiple-choice prompt with custom values.
 * 
 * @author John Jenkins
 */
public class MultiChoiceCustomPromptResponse extends PromptResponse {
	private final List<String> choices;
	
	/**
	 * Creates a response to a multiple-choice prompt with custom choices.
	 * 
	 * @param prompt The HoursBeforeNowPrompt used to generate this response.
	 * 
	 * @param noResponse A 
	 * 					 {@link org.ohmage.domain.campaign.Response.NoResponse}
	 * 					 value if the user didn't supply an answer to this 
	 * 					 prompt.
	 * 
	 * @param repeatableSetIteration If the prompt was part of a repeatable 
	 * 								 set, this is the iteration of that 
	 * 								 repeatable set on which this response was
	 * 								 made.
	 * 
	 * @param choices The response from the user.
	 * 
	 * @param validate Whether or not to validate the response.
	 * 
	 * @throws IllegalArgumentException Thrown if any of the parameters are 
	 * 									invalid or if 'validate' is "true" and
	 * 									the response value is invalid.
	 */
	public MultiChoiceCustomPromptResponse(
			final MultiChoiceCustomPrompt prompt, final NoResponse noResponse, 
			final Integer repeatableSetIteration, 
			final Collection<String> choices,
			final boolean validate) {
		
		super(prompt, noResponse, repeatableSetIteration);
		
		if((choices == null) && (noResponse == null)) {
			throw new IllegalArgumentException("Both hours and no response are null.");
		}
		else if((choices != null) && (noResponse != null)) {
			throw new IllegalArgumentException("Both hours and no response were given.");
		}
		
		if(validate) {
			prompt.validateValue(choices);
		}
		this.choices = new ArrayList<String>(choices);
	}
	
	/**
	 * Returns the choices from the user.
	 * 
	 * @return The choices from the user.
	 */
	public List<String> getChoices() {
		return choices;
	}

	/**
	 * Returns the choices as a string.
	 * 
	 * @return The choices as a String object.
	 */
	@Override
	public String getResponseValue() {
		String noResponseString = super.getResponseValue();
		
		if(noResponseString == null) {
			return choices.toString();
		}
		else {
			return noResponseString;
		}		
	}

	/**
	 * Generates a hash code for this response.
	 * 
	 * @return A hash code for this prompt response.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((choices == null) ? 0 : choices.hashCode());
		return result;
	}

	/**
	 * Determines if this prompt response is logically equivalent to another
	 * object.
	 * 
	 * @param obj The other object.
	 * 
	 * @return True if this response is logically equivalent to the other 
	 * 		   object; false, otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MultiChoiceCustomPromptResponse other = (MultiChoiceCustomPromptResponse) obj;
		if (choices == null) {
			if (other.choices != null)
				return false;
		} else if (!choices.equals(other.choices))
			return false;
		return true;
	}
}