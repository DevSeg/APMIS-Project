package com.cinoteck.application.views.campaign;

import java.io.IOException;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dnd.internal.DndUtil;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.importexport.ImportFacade;
import de.symeda.sormas.api.infrastructure.InfrastructureType;


public class CampaignDataImportDialog extends Dialog{
	
	ComboBox campaignFilter = new ComboBox<>();
	Button downloadImportTemplate = new Button(I18nProperties.getCaption(Captions.downloadImportTemplate));
	Button startDataImport = new Button(I18nProperties.getCaption(Captions.startDataImport));
	Button donloadErrorReport = new Button(I18nProperties.getCaption(Captions.downloadErrorReport));
	ComboBox valueSeperator = new ComboBox<>();
	
	public CampaignDataImportDialog() {
		this.setHeaderTitle(I18nProperties.getCaption(Captions.importCampaignFormData));
//		this.getStyle().set("color" , "#0D6938");
		
		Hr seperatorr = new Hr();
		seperatorr.getStyle().set("color" , " #0D6938");
		
		
	VerticalLayout dialog = new VerticalLayout();
	
//		campaignFilter.setId(CampaignDto.NAME);
//		campaignFilter.setRequired(true);
//		campaignFilter.setItems(FacadeProvider.getCampaignFacade().getAllActiveCampaignsAsReference());
//		campaignFilter.setEnabled(false);
//		
//		Label lblCollectionDateInfo = new Label(I18nProperties.getString(Strings.infoPopulationCollectionDate));
		
	H3 step2 = new H3();
	step2.add(I18nProperties.getString(Strings.step1));
	Label lblImportTemplateInfo = new Label(I18nProperties.getString(Strings.step1Description));
	downloadImportTemplate.addClickListener(null);
	
	H3 step3 = new H3();
	step3.add(I18nProperties.getString(Strings.step2));
	Label lblImportCsvFile = new Label(I18nProperties.getString(Strings.stepDesciption));
	startDataImport.addClickListener(null);
	
	
	H3 step4 = new H3();
	step4.add(I18nProperties.getString(Strings.step3));
	Label lblDnldErrorReport = new Label(I18nProperties.getString(Strings.step3Description));
	donloadErrorReport.addClickListener(null);
	
	
	
		dialog.add(seperatorr, step2, lblImportTemplateInfo, 
				downloadImportTemplate, step3, lblImportCsvFile, startDataImport, step4, lblDnldErrorReport,donloadErrorReport);
	add(dialog);
		
	}
	

}
