/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.ui.campaign;

import static com.vaadin.v7.data.Validator.InvalidValueException;
import static de.symeda.sormas.api.campaign.data.CampaignFormDataCriteria.CAMPAIGN;

import java.time.ZonedDateTime;

import com.vaadin.server.ConnectorResource;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.ui.BrowserWindowOpenerState;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.campaign.campaigndata.CampaignDataView;
import de.symeda.sormas.ui.campaign.campaigndata.CampaignFormDataEditForm;
import de.symeda.sormas.ui.campaign.campaigndata.CampaignFormDataView;
import de.symeda.sormas.ui.campaign.campaigns.CampaignEditForm;
import de.symeda.sormas.ui.campaign.campaigns.CampaignView;
import de.symeda.sormas.ui.campaign.campaigns.CampaignsView;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

public class CampaignController {

	public void createOrEditCampaign(String uuid) {//sormas-logo

		CommitDiscardWrapperComponent<CampaignEditForm> campaignComponent;
		String heading;
		if (uuid != null) {
			CampaignDto campaign = getCampaign(uuid); 
			campaignComponent = getCampaignComponent(getCampaign(uuid), () -> {
				Notification.show(I18nProperties.getString(Strings.messageCampaignSaved), Type.WARNING_MESSAGE);
				SormasUI.refreshView(); 
			});

			if (UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_DELETE)) {
				campaignComponent.addDeleteListener(() -> {
					FacadeProvider.getCampaignFacade().deleteCampaign(campaign.getUuid());
					campaignComponent.discard();
					SormasUI.refreshView();
				}, I18nProperties.getString(Strings.entityCampaign));
			}

			if (UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_DELETE)) {
				campaignComponent.addCloneListener(() -> {
					String newUUId = FacadeProvider.getCampaignFacade().cloneCampaign(campaign.getUuid(), UserProvider.getCurrent().getUuid());
					campaignComponent.discard();
				//	UI.getCurrent().getNavigator().navigateTo(CampaignDataView.VIEW_NAME + "/?" + CAMPAIGN + "=" + newUUId);

					//SormasUI.refreshView();
				}, I18nProperties.getString(Strings.entityCampaign));
			}
//			
			
//			if (UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_DELETE)) {
//				campaignComponent.addCloseOpenListener(() -> {
//					
//					FacadeProvider.getCampaignFacade().closeandOpenCampaign(campaign.getUuid(), false);
//					campaignComponent.discard();
//					SormasUI.refreshView();
//				
//				}, I18nProperties.getString(Strings.entityCampaign));
//			}
//			
//			
//			if (UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_DELETE)) {
//				campaignComponent.addOpenCloseListener(() -> {
//					FacadeProvider.getCampaignFacade().closeandOpenCampaign(campaign.getUuid(), true);
//					campaignComponent.discard();
//					SormasUI.refreshView();
//				}, I18nProperties.getString(Strings.entityCampaign));
//			}
//			
			if (UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_DELETE)) {
				final String campaignUuid = campaign.getUuid();
				boolean closex = FacadeProvider.getCampaignFacade().isClosedd(campaignUuid);
				if(closex) {
				campaignComponent.addCloseOpenListener(() -> {
					FacadeProvider.getCampaignFacade().closeandOpenCampaign(campaignUuid, true);
					campaignComponent.discard();
					SormasUI.refreshView();
				}, I18nProperties.getString(Strings.entityCampaign));
				
				}else {
					campaignComponent.addOpenCloseListener(() -> {
						FacadeProvider.getCampaignFacade().closeandOpenCampaign(campaignUuid, false);
						campaignComponent.discard();
						SormasUI.refreshView();
					}, I18nProperties.getString(Strings.entityCampaign));
					
				}
			}
			
			

			// Initialize 'Archive' button
			if (UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_ARCHIVE)) {
				boolean archived = FacadeProvider.getCampaignFacade().isArchived(campaign.getUuid());
				Button archiveCampaignButton = ButtonHelper.createButton(archived ? Captions.actionDearchive : Captions.actionArchive, e -> {
					campaignComponent.commit();
					archiveOrDearchiveCampaign(campaign.getUuid(), !archived);
				}, ValoTheme.BUTTON_LINK);

				campaignComponent.getButtonsPanel().addComponentAsFirst(archiveCampaignButton);
				campaignComponent.getButtonsPanel().setComponentAlignment(archiveCampaignButton, Alignment.BOTTOM_LEFT);
			}
			heading = I18nProperties.getString(Strings.headingEditCampaign);
		} else {
			campaignComponent = getCampaignComponent(null, () -> {
				Notification.show(I18nProperties.getString(Strings.messageCampaignCreated), Type.WARNING_MESSAGE);
				SormasUI.refreshView();
			});
			heading = I18nProperties.getString(Strings.headingCreateNewCampaign);
		}
		VaadinUiUtil.showModalPopupWindow(campaignComponent, heading);
	}

	public void createCampaignDataForm(CampaignReferenceDto campaign, CampaignFormMetaReferenceDto campaignForm) {
		Window window = VaadinUiUtil.createPopupWindow();
		
		CommitDiscardWrapperComponent<CampaignFormDataEditForm> component =
			getCampaignFormDataComponent(null, campaign, campaignForm, false, false, () -> {
				window.close();
				SormasUI.refreshView(); //Strings.messageCampaignFormSaved
				Notification
					.show(String.format(I18nProperties.getString(Strings.messageCampaignFormSaved), campaignForm.toString()), Type.TRAY_NOTIFICATION);
			}, window::close, () -> {
				SormasUI.refreshView(); //Strings.messageCampaignFormSaved
				Notification
					.show(String.format(I18nProperties.getString(Strings.messageCampaignFormSavedandContinue), campaignForm.toString()), Type.TRAY_NOTIFICATION);
				
			},true);

		window.setCaption(String.format(I18nProperties.getString(Strings.headingCreateCampaignDataForm), campaignForm.toString()));
		window.setContent(component);
		UI.getCurrent().addWindow(window);
	}

	private void archiveOrDearchiveCampaign(String campaignUuid, boolean archive) {

		if (archive) {
			Label contentLabel = new Label(
				String.format(
					I18nProperties.getString(Strings.confirmationArchiveCampaign),
					I18nProperties.getString(Strings.entityCampaign).toLowerCase(),
					I18nProperties.getString(Strings.entityCampaign).toLowerCase()));
			VaadinUiUtil.showConfirmationPopup(
				I18nProperties.getString(Strings.headingArchiveCampaign),
				contentLabel,
				I18nProperties.getString(Strings.yes),
				I18nProperties.getString(Strings.no),
				640,
				e -> {
					if (e) {
						FacadeProvider.getCampaignFacade().archiveOrDearchiveCampaign(campaignUuid, true);
						SormasUI.refreshView();
					}
				});
		} else {
			Label contentLabel = new Label(
				String.format(
					I18nProperties.getString(Strings.confirmationDearchiveCampaign),
					I18nProperties.getString(Strings.entityCampaign).toLowerCase(),
					I18nProperties.getString(Strings.entityCampaign).toLowerCase()));
			VaadinUiUtil.showConfirmationPopup(
				I18nProperties.getString(Strings.headingDearchiveCampaign),
				contentLabel,
				I18nProperties.getString(Strings.yes),
				I18nProperties.getString(Strings.no),
				640,
				e -> {
					if (e) {
						FacadeProvider.getCampaignFacade().archiveOrDearchiveCampaign(campaignUuid, false);
						SormasUI.refreshView();
					}
				});
		}
	}

	public CommitDiscardWrapperComponent<CampaignEditForm> getCampaignComponent(CampaignDto campaignDto, Runnable callback) {
		
		CampaignEditForm campaignEditForm = new CampaignEditForm(campaignDto);
		boolean isCreate = false;
		if (campaignDto == null) {
			isCreate = true;
			campaignDto = CampaignDto.build();
			campaignDto.setCreatingUser(UserProvider.getCurrent().getUserReference());
		}
		campaignEditForm.setValue(campaignDto);

		final CommitDiscardWrapperComponent<CampaignEditForm> campaignComponent = new CommitDiscardWrapperComponent<CampaignEditForm>(
				
			campaignEditForm,
			UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_EDIT),
			campaignEditForm.getFieldGroup()) {
			

			@Override
			public void discard() {
				super.discard();
				campaignEditForm.discard();
			}
			
		};

		if (UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_DELETE) && !isCreate) {
			CampaignDto finalCampaignDto = campaignDto;
			campaignComponent.addDeleteListener(() -> {
				FacadeProvider.getCampaignFacade().deleteCampaign(finalCampaignDto.getUuid());
				UI.getCurrent().getNavigator().navigateTo(CampaignsView.VIEW_NAME);
			}, I18nProperties.getString(Strings.entityCampaign));
		}
		
		//Clone
		if (UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_DELETE) && !isCreate) {
			CampaignDto finalCampaignDto = campaignDto;
			campaignComponent.addCloneListener(() -> {
			
				String newUuid = FacadeProvider.getCampaignFacade().cloneCampaign(finalCampaignDto.getUuid(), UserProvider.getCurrent().getUuid());
				UI.getCurrent().getNavigator().navigateTo(CampaignView.VIEW_NAME + "/" + newUuid);
				Notification.show("Success", "Campaign has been duplicated succesfully.", Notification.TYPE_TRAY_NOTIFICATION);

				//UI.getCurrent().getNavigator().navigateTo(CampaignsView.VIEW_NAME);
			}, I18nProperties.getString(Strings.entityCampaign));
		}
		
		if (UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_DELETE)) {
			final String campaignUuid = campaignDto.getUuid();
			CampaignDto finalCampaignDto = campaignDto;
			
			boolean closex = FacadeProvider.getCampaignFacade().isClosedd(campaignUuid);
			if(closex) {
			campaignComponent.addCloseOpenListener(() -> {
				
				FacadeProvider.getCampaignFacade().closeandOpenCampaign(finalCampaignDto.getUuid(), false);
				campaignComponent.discard();
				SormasUI.refreshView();
			}, I18nProperties.getString(Strings.entityCampaign));
			
			
			}else {
				campaignComponent.addOpenCloseListener(() -> {
					FacadeProvider.getCampaignFacade().closeandOpenCampaign(finalCampaignDto.getUuid(), true);
					campaignComponent.discard();
					SormasUI.refreshView();
				}, I18nProperties.getString(Strings.entityCampaign));
				
			}
		}
		
		
		

		// Initialize 'Archive' button
		if (UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_ARCHIVE) && !isCreate) {
			final String campaignUuid = campaignDto.getUuid();
			boolean archived = FacadeProvider.getCampaignFacade().isArchived(campaignUuid);
			Button archiveCampaignButton = ButtonHelper.createButton(archived ? Captions.actionDearchive : Captions.actionArchive, e -> {
				campaignComponent.commit();
				archiveOrDearchiveCampaign(campaignUuid, !archived);
			}, ValoTheme.BUTTON_LINK);

			campaignComponent.getButtonsPanel().addComponentAsFirst(archiveCampaignButton);
			campaignComponent.getButtonsPanel().setComponentAlignment(archiveCampaignButton, Alignment.BOTTOM_LEFT);
		}

		campaignComponent.addCommitListener(() -> {
			if (!campaignEditForm.getFieldGroup().isModified()) {
				CampaignDto dto = campaignEditForm.getValue();
				FacadeProvider.getCampaignFacade().saveCampaign(dto);
				SormasUI.refreshView();
				callback.run();
			}
		});

		return campaignComponent;
	}
	
	public CommitDiscardWrapperComponent<CampaignFormDataEditForm> getCampaignFormDataComponent(
			CampaignFormDataDto campaignFormData,
			CampaignReferenceDto campaign,
			CampaignFormMetaReferenceDto campaignForm,
			boolean revertFormOnDiscard,
			boolean showDeleteButton,
			Runnable commitCallback,
			Runnable discardCallback) {
		
		

			CampaignFormDataEditForm form = new CampaignFormDataEditForm(campaignFormData == null);
			if (campaignFormData == null) {

				final UserDto currentUser = UserProvider.getCurrent().getUser();
				campaignFormData =
					CampaignFormDataDto.build(campaign, campaignForm, currentUser.getArea(), currentUser.getRegion(), currentUser.getDistrict(), null);
				campaignFormData.setCreatingUser(UserProvider.getCurrent().getUserReference());
			}
			form.setValue(campaignFormData);

			final CommitDiscardWrapperComponent<CampaignFormDataEditForm> component = new CommitDiscardWrapperComponent<>(form, form.getFieldGroup());

			component.addCommitListener(() -> {
				if (!form.getFieldGroup().isModified()) {
					try {
						form.validate();
					} catch (InvalidValueException e) {
						Notification.show(I18nProperties.getValidationError(Validations.errorsInForm), Type.ERROR_MESSAGE);
						return;
					}

					CampaignFormDataDto formData = form.getValue();
					FacadeProvider.getCampaignFormDataFacade().saveCampaignFormData(formData);
					if (commitCallback != null) {
						commitCallback.run();
						UI.getCurrent().getNavigator().navigateTo(CampaignDataView.VIEW_NAME + "/?" + CAMPAIGN + "=" + campaign.getUuid());
					}
				}
			});

			component.addDiscardListener(
				() -> UI.getCurrent().getNavigator().navigateTo(CampaignDataView.VIEW_NAME + "/?" + CAMPAIGN + "=" + campaign.getUuid()));

			if (revertFormOnDiscard) {
				component.addDiscardListener(form::resetFormValues);
			}

			if (discardCallback != null) {
				component.addDiscardListener(discardCallback::run);
			}

			if (showDeleteButton && UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_DELETE)) {
				String campaignFormDataUuid = campaignFormData.getUuid();

				component.addDeleteListener(() -> {
					FacadeProvider.getCampaignFormDataFacade().deleteCampaignFormData(campaignFormDataUuid);
					UI.getCurrent().getNavigator().navigateTo(CampaignFormDataView.VIEW_NAME);
				}, I18nProperties.getString(Strings.entityCampaignDataForm));
			}
			
		

			return component;
		}
	

	public CommitDiscardWrapperComponent<CampaignFormDataEditForm> getCampaignFormDataComponent(
		CampaignFormDataDto campaignFormData,
		CampaignReferenceDto campaign,
		CampaignFormMetaReferenceDto campaignForm,
		boolean revertFormOnDiscard,
		boolean showDeleteButton,
		Runnable commitCallback,
		Runnable discardCallback,
		Runnable saveandcontdCallback,
		boolean showCloneButton) {

		CampaignFormDataEditForm form = new CampaignFormDataEditForm(campaignFormData == null);
		if (campaignFormData == null) {

			final UserDto currentUser = UserProvider.getCurrent().getUser();
			
			
			CommunityReferenceDto u = new CommunityReferenceDto();
			if(currentUser.getCommunity() != null) {
			//u = currentUser.getCommunity().iterator().next();
			}
			campaignFormData =
				CampaignFormDataDto.build(campaign, campaignForm, currentUser.getArea(), currentUser.getRegion(),  currentUser.getDistrict(), null);
			campaignFormData.setCreatingUser(UserProvider.getCurrent().getUserReference());
		}
		final UserDto currentUsex = UserProvider.getCurrent().getUser();
		form.setValue(campaignFormData);

		final CommitDiscardWrapperComponent<CampaignFormDataEditForm> component = new CommitDiscardWrapperComponent<>(form, form.getFieldGroup());

		component.addCommitListener(() -> {
			System.out.println("))))saving)))))");
			if (!form.getFieldGroup().isModified()) {
				
				try {
					form.validate();
				} catch (InvalidValueException e) {
					Notification.show(I18nProperties.getValidationError(Validations.errorsInForm), Type.ERROR_MESSAGE);
					return;
				}
				
				
				CampaignFormDataDto formData = form.getValue();
				FacadeProvider.getCampaignFormDataFacade().saveCampaignFormData(formData);
				if (commitCallback != null) {
					commitCallback.run();
					UI.getCurrent().getNavigator().navigateTo(CampaignDataView.VIEW_NAME + "/?" + CAMPAIGN + "=" + campaign.getUuid());
				}
			}
		});
		
		
		
		 // TODO duplicate form
		component.addCommitandContListener(() -> {
			System.out.println("))))))))))))))))");
			if (!form.getFieldGroup().isModified()) {
				try {
					form.validate();
				} catch (InvalidValueException e) {
					Notification.show(I18nProperties.getValidationError(Validations.errorsInForm), Type.ERROR_MESSAGE);
					return;
				}

				CampaignFormDataDto formData = form.getValue();
				FacadeProvider.getCampaignFormDataFacade().saveCampaignFormData(formData);
				if (saveandcontdCallback != null) {
					
					System.out.println("))))))))))))))))))))))))))) = "+campaign.getUuid() +"    _______      "+campaignForm.getUuid());
					
					saveandcontdCallback.run();
					form.resetFormValues();
					
					discardCallback.run();
					
					
					
					ControllerProvider.getCampaignController().navigateToFormDataView(campaign.getUuid(), campaignForm.getUuid());
					//ControllerProvider.getCampaignController().createCampaignDataForm(campaign, campaignForm);
					
					//UI.getCurrent().getNavigator().navigateTo(CampaignDataView.VIEW_NAME + "/?" + CAMPAIGN + "=" + campaign.getUuid());
				}
			}
			
		},  I18nProperties.getString(Strings.entityCampaignDataForm));

		
		
		
		
		

		component.addDiscardListener(
			() -> UI.getCurrent().getNavigator().navigateTo(CampaignDataView.VIEW_NAME + "/?" + CAMPAIGN + "=" + campaign.getUuid()));

		if (revertFormOnDiscard) {
			component.addDiscardListener(form::resetFormValues);
		}

		if (discardCallback != null) {
			component.addDiscardListener(discardCallback::run);
		}

		if (showDeleteButton && UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_DELETE)) {
			String campaignFormDataUuid = campaignFormData.getUuid();

			component.addDeleteListener(() -> {
				FacadeProvider.getCampaignFormDataFacade().deleteCampaignFormData(campaignFormDataUuid);
				UI.getCurrent().getNavigator().navigateTo(CampaignFormDataView.VIEW_NAME);
			}, I18nProperties.getString(Strings.entityCampaignDataForm));
		}
		
		
		

		return component;
	}

	private CampaignDto getCampaign(String uuid) {
		return FacadeProvider.getCampaignFacade().getByUuid(uuid);
	}

	public void navigateToCampaign(String uuid) {
		String navigationState = CampaignView.VIEW_NAME + "/" + uuid;
		SormasUI.get().getNavigator().navigateTo(navigationState);
	}

	public void navigateToFormDataView(String uuid) {
		String navigationState = CampaignFormDataView.VIEW_NAME + "/" + uuid;
		SormasUI.get().getNavigator().navigateTo(navigationState);
	}
	
	public void navigateToFormDataViewDRAFT(String camPuuid, String formUUID) {
		String navigationState = CampaignFormDataView.VIEW_NAME + "/"+camPuuid+","+formUUID;
		//SormasUI.get().getNavigator().navigateTo(navigationState);
		
		String generatedURL = ApplicationConstants.APP_PATH+"/"+ConnectorResource.CONNECTOR_PATH+"/"+
                UI.getCurrent().getUIId()+"/"+
                BrowserWindowOpenerState.locationResource+"/"+navigationState;
		
		
//Page.getCurrent().open(generatedURL, "_blank");

	//	SormasUI.get().getNavigator().navigateTo(navigationState);
	}
	
	public void navigateToFormDataView(String camPuuid, String formUUID) {
		boolean closex = FacadeProvider.getCampaignFacade().isClosedd(camPuuid);
		if(!closex) {
			String navigationState = CampaignFormDataView.VIEW_NAME + "/"+camPuuid+","+formUUID;
			SormasUI.get().getNavigator().navigateTo(navigationState);
		
		}else {
			Notification.show("Closed Campaign", "We are no longer accepting data for this campaign at this time. Please contact your supervisor for further information and support. Thank you.", Notification.TYPE_WARNING_MESSAGE);
			
		}
		
		}

	public void navigateToCampaignData(String campaignUuid) {
		String navigationState = CampaignDataView.VIEW_NAME + "/?" + CampaignFormDataDto.CAMPAIGN + "=" + campaignUuid;
		SormasUI.get().getNavigator().navigateTo(navigationState);
	}
}
