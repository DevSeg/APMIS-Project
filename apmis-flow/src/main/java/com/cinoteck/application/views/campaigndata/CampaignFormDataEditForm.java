package com.cinoteck.application.views.campaigndata;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import de.symeda.sormas.api.campaign.data.CampaignFormDataDto;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;




public class CampaignFormDataEditForm extends FormLayout{
	
	Binder<CampaignFormDataDto> binder = new BeanValidationBinder<>(CampaignFormDataDto.class);
	 ComboBox<CampaignReferenceDto> cbCampaign = new ComboBox<>(CampaignFormDataDto.CAMPAIGN);
	 
	 ComboBox<CampaignReferenceDto> cbRegion = new ComboBox<>(CampaignFormDataDto.REGION);
	 ComboBox<CampaignReferenceDto> cbArea = new ComboBox<>(CampaignFormDataDto.AREA);
	 ComboBox<CampaignReferenceDto> cbDistrict = new ComboBox<>(CampaignFormDataDto.DISTRICT);
	 ComboBox<CampaignReferenceDto> cbCommunity = new ComboBox<>(CampaignFormDataDto.COMMUNITY);
	
	public  CampaignFormDataEditForm() {
		configureFields();
	}
	
	private void configureFields() {
		binder.forField(cbCampaign).bind(CampaignFormDataDto::getCampaign, CampaignFormDataDto::setCampaign);
		binder.forField(cbRegion).bind(CampaignFormDataDto::getCampaign, CampaignFormDataDto::setCampaign);
	add(cbCampaign,cbRegion);
	}

}
