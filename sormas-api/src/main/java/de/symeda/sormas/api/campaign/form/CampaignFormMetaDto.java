package de.symeda.sormas.api.campaign.form;

import java.util.List;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.symeda.sormas.api.EntityDto;
import de.symeda.sormas.api.Modality;
import de.symeda.sormas.api.campaign.CampaignPhase;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.user.FormAccess;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.FieldConstraints;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignFormMetaDto extends EntityDto {

	private static final long serialVersionUID = -1163673887940552133L;

	public static final String FORM_ID = "formId";
	public static final String FORM_NAME = "formName";
	public static final String LANGUAGE_CODE = "languageCode";
	public static final String CAMPAIGN_FORM_ELEMENTS = "campaignFormElements";
	public static final String FORM_CATEGORY = "formCategory";
	public static final String DAYSTOEXPIRE = "daysExpired";
	public static final String DISTRICTENTRY = "districtentry";
	public static final String FORM_TYPE = "formType";
	public static final String MODALITY = "modality";

	@Size(max = FieldConstraints.CHARACTER_LIMIT_SMALL, message = Validations.textTooLong)
	private String formId;
	@Size(max = FieldConstraints.CHARACTER_LIMIT_DEFAULT, message = Validations.textTooLong)
	private String formName;
	@Size(max = FieldConstraints.CHARACTER_LIMIT_SMALL, message = Validations.textTooLong)
	private String languageCode;	
	@Valid
	private List<CampaignFormElement> campaignFormElements;
	@Valid
	private List<CampaignFormTranslations> campaignFormTranslations;
	
	@Enumerated(EnumType.STRING)
	private CampaignPhase formType;	
	
	@Enumerated(EnumType.STRING)
	private Modality modality;
	
	@Enumerated(EnumType.STRING)
	private FormAccess formCategory;
	private int daysExpired;

	private boolean districtentry = false;

	public static CampaignFormMetaDto build() {
		CampaignFormMetaDto campaignMeta = new CampaignFormMetaDto();
		campaignMeta.setUuid(DataHelper.createUuid());
		return campaignMeta;
	}
	
	public String getFormId() {
		return formId;
	}

	public void setFormId(String formId) {
		this.formId = formId;
	}

	public String getFormName() {
		return formName;
	}

	public void setFormName(String formName) {
		this.formName = formName;
	}

	public String getLanguageCode() {
		return languageCode;
	}

	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}

	public List<CampaignFormElement> getCampaignFormElements() {
		return campaignFormElements;
	}

	public void setCampaignFormElements(List<CampaignFormElement> campaignFormElements) {
		this.campaignFormElements = campaignFormElements;
	}

	public List<CampaignFormTranslations> getCampaignFormTranslations() {
		return campaignFormTranslations;
	}

	public void setCampaignFormTranslations(List<CampaignFormTranslations> campaignFormTranslations) {
		this.campaignFormTranslations = campaignFormTranslations;
	}

	public FormAccess getFormCategory() {
		return formCategory;
	}

	public void setFormCategory(FormAccess formCategory) {
		this.formCategory = formCategory;
	}
	
	public CampaignPhase getFormType() {
		return formType;
	}

	public void setFormType(CampaignPhase formType) {
		this.formType = formType;
	}
	
	public Modality getModality() {
		return modality;
	}

	public void setModality(Modality modality) {
		this.modality = modality;
	}

	public int getDaysExpired() {
		return daysExpired;
	}

	public void setDaysExpired(int daysExpired) {
		this.daysExpired = daysExpired;
	}

	public boolean isDistrictentry() {
		return districtentry;
	}

	public void setDistrictentry(boolean districtentry) {
		this.districtentry = districtentry;
	}
}
