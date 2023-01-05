/*
 * ******************************************************************************
 * * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * *
 * * This program is free software: you can redistribute it and/or modify
 * * it under the terms of the GNU General Public License as published by
 * * the Free Software Foundation, either version 3 of the License, or
 * * (at your option) any later version.
 * *
 * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * * GNU General Public License for more details.
 * *
 * * You should have received a copy of the GNU General Public License
 * * along with this program. If not, see <https://www.gnu.org/licenses/>.
 * ******************************************************************************
 */

package de.symeda.sormas.backend.campaign.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.vladmihalcea.hibernate.type.util.SQLExtractor;

import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.CampaignJurisdictionLevel;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.api.campaign.data.CampaignFormDataDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.data.CampaignFormDataFacade;
import de.symeda.sormas.api.campaign.data.CampaignFormDataIndexDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataReferenceDto;
import de.symeda.sormas.api.campaign.data.MapCampaignDataDto;
import de.symeda.sormas.api.campaign.diagram.CampaignDiagramCriteria;
import de.symeda.sormas.api.campaign.diagram.CampaignDiagramDataDto;
import de.symeda.sormas.api.campaign.diagram.CampaignDiagramSeries;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.campaign.form.CampaignFormElementType;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaDto;
import de.symeda.sormas.api.caze.MapCaseDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.infrastructure.PopulationDataCriteria;
import de.symeda.sormas.api.infrastructure.PopulationDataDto;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.community.CommunityDto;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.FormAccess;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.SortProperty;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.backend.campaign.Campaign;
import de.symeda.sormas.backend.campaign.CampaignFacadeEjb;
import de.symeda.sormas.backend.campaign.CampaignService;
import de.symeda.sormas.backend.campaign.form.CampaignFormMeta;
import de.symeda.sormas.backend.campaign.form.CampaignFormMetaFacadeEjb;
import de.symeda.sormas.backend.campaign.form.CampaignFormMetaService;
import de.symeda.sormas.backend.caze.Case;
import de.symeda.sormas.backend.caze.CaseQueryContext;
import de.symeda.sormas.backend.common.AbstractDomainObject;
import de.symeda.sormas.backend.common.CriteriaBuilderHelper;
import de.symeda.sormas.backend.infrastructure.PopulationDataFacadeEjb;
import de.symeda.sormas.backend.infrastructure.area.Area;
import de.symeda.sormas.backend.infrastructure.area.AreaFacadeEjb;
import de.symeda.sormas.backend.infrastructure.area.AreaService;
import de.symeda.sormas.backend.infrastructure.community.Community;
import de.symeda.sormas.backend.infrastructure.community.CommunityFacadeEjb;
import de.symeda.sormas.backend.infrastructure.community.CommunityService;
import de.symeda.sormas.backend.infrastructure.district.District;
import de.symeda.sormas.backend.infrastructure.district.DistrictFacadeEjb;
import de.symeda.sormas.backend.infrastructure.district.DistrictService;
import de.symeda.sormas.backend.infrastructure.facility.Facility;
import de.symeda.sormas.backend.infrastructure.region.Region;
import de.symeda.sormas.backend.infrastructure.region.RegionFacadeEjb;
import de.symeda.sormas.backend.infrastructure.region.RegionService;
import de.symeda.sormas.backend.location.Location;
import de.symeda.sormas.backend.person.Person;
import de.symeda.sormas.backend.user.UserFacadeEjb;
import de.symeda.sormas.backend.user.UserService;
import de.symeda.sormas.backend.util.DtoHelper;
import de.symeda.sormas.backend.util.JurisdictionHelper;
import de.symeda.sormas.backend.util.ModelConstants;
import de.symeda.sormas.backend.util.QueryHelper;
import de.symeda.sormas.utils.CaseJoins;

@Stateless(name = "CampaignFormDataFacade")
public class CampaignFormDataFacadeEjb implements CampaignFormDataFacade {

	@PersistenceContext(unitName = ModelConstants.PERSISTENCE_UNIT_NAME)
	private EntityManager em;

	@EJB
	private CampaignFormDataService campaignFormDataService;

	@EJB
	private CampaignService campaignService;

	@EJB
	private CampaignFormMetaService campaignFormMetaService;

	@EJB
	private RegionService regionService;

	@EJB
	private DistrictService districtService;

	@EJB
	private CommunityService communityService;

	@EJB
	private UserService userService;

	@EJB
	private PopulationDataFacadeEjb.PopulationDataFacadeEjbLocal populationDataFacadeEjb;

	@EJB
	private AreaService areaService;

	@EJB
	private AreaFacadeEjb.AreaFacadeEjbLocal areaFacadeEjb;

	@EJB
	private RegionFacadeEjb.RegionFacadeEjbLocal regionFacadeEjb;

	@EJB
	private DistrictFacadeEjb.DistrictFacadeEjbLocal districtFacadeEjb;

	public CampaignFormData fromDto(@NotNull CampaignFormDataDto source, boolean checkChangeDate) {
		CampaignFormData target = DtoHelper.fillOrBuildEntity(source, campaignFormDataService.getByUuid(source.getUuid()), CampaignFormData::new, checkChangeDate);

		target.setFormValues(source.getFormValues());
		target.setCampaign(campaignService.getByReferenceDto(source.getCampaign()));
		target.setCampaignFormMeta(campaignFormMetaService.getByReferenceDto(source.getCampaignFormMeta()));
		target.setFormDate(source.getFormDate());
		target.setArea(areaService.getByReferenceDto(source.getArea()));
		target.setRegion(regionService.getByReferenceDto(source.getRegion()));
		target.setDistrict(districtService.getByReferenceDto(source.getDistrict()));
		target.setCommunity(communityService.getByReferenceDto(source.getCommunity()));
		target.setCreatingUser(userService.getByReferenceDto(source.getCreatingUser()));
	//	target.setLat(source.getLatitude());
		//target.setLon(source.getLongitude());
		return target;
	}

	public CampaignFormDataDto toDto(CampaignFormData source) {
		if (source == null) {
			return null;
		}

		CampaignFormDataDto target = new CampaignFormDataDto();
		DtoHelper.fillDto(target, source);

		target.setFormValues(source.getFormValues());
		target.setCampaign(CampaignFacadeEjb.toReferenceDto(source.getCampaign()));
		target.setCampaignFormMeta(CampaignFormMetaFacadeEjb.toReferenceDto(source.getCampaignFormMeta()));
		target.setFormDate(source.getFormDate());
		target.setArea(AreaFacadeEjb.toReferenceDto(source.getArea()));
		target.setRegion(RegionFacadeEjb.toReferenceDto(source.getRegion()));
		target.setDistrict(DistrictFacadeEjb.toReferenceDto(source.getDistrict()));
		target.setCommunity(CommunityFacadeEjb.toReferenceDto(source.getCommunity()));
		target.setCreatingUser(UserFacadeEjb.toReferenceDto(source.getCreatingUser()));
	//	target.setLatitude(source.getLat());
	//	target.setLongitude(source.getLon());
		return target;
	}

	@Override
	public CampaignFormDataDto saveCampaignFormData(@Valid CampaignFormDataDto campaignFormDataDto)
			throws ValidationRuntimeException {

		CampaignFormData campaignFormData = fromDto(campaignFormDataDto, true);
		CampaignFormDataEntry.removeNullValueEntries(campaignFormData.getFormValues());

		validate(campaignFormDataDto);

		campaignFormDataService.ensurePersisted(campaignFormData);
		return toDto(campaignFormData);
	}

	private void validate(CampaignFormDataDto campaignFormDataDto) {
		if (campaignFormDataDto.getCampaign() == null) {
			throw new ValidationRuntimeException(I18nProperties.getValidationError("Campaign_id now valid!"));
		}
		if (campaignFormDataDto.getArea() == null) {
			throw new ValidationRuntimeException(I18nProperties.getValidationError(Validations.validArea));
		}
		if (campaignFormDataDto.getRegion() == null) {
			throw new ValidationRuntimeException(I18nProperties.getValidationError(Validations.validRegion));
		}
		if (campaignFormDataDto.getDistrict() == null) {
			throw new ValidationRuntimeException(I18nProperties.getValidationError(Validations.validDistrict));
		}
		if (campaignFormDataDto.getCommunity() == null) {
			throw new ValidationRuntimeException(I18nProperties.getValidationError(Validations.validCommunity));
		}
	}

	@Override
	public List<CampaignFormDataDto> getByUuids(List<String> uuids) {
		return campaignFormDataService.getByUuids(uuids).stream().map(c -> convertToDto(c))
				.collect(Collectors.toList());
	}

	@Override
	public void deleteCampaignFormData(String campaignFormDataUuid) {
		if (!userService.hasRight(UserRight.CAMPAIGN_FORM_DATA_DELETE)) {
			throw new UnsupportedOperationException(
					"User " + userService.getCurrentUser().getUuid() + "is not allowed to delete Campaign Form Data");
		}

		CampaignFormData campaignFormData = campaignFormDataService.getByUuid(campaignFormDataUuid);
		campaignFormDataService.delete(campaignFormData);
	}

	private CampaignFormDataDto convertToDto(CampaignFormData source) {
		CampaignFormDataDto dto = toDto(source);
		return dto;
	}

	@Override
	public CampaignFormDataDto getCampaignFormDataByUuid(String uuid) {
		return toDto(campaignFormDataService.getByUuid(uuid));
	}

	@Override
	public boolean isArchived(String campaignFormDataUuid) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<CampaignFormData> from = cq.from(CampaignFormData.class);

		cq.where(cb.and(cb.equal(from.get(CampaignFormData.ARCHIVED), true),
				cb.equal(from.get(AbstractDomainObject.UUID), campaignFormDataUuid)));
		cq.select(cb.count(from));
		long count = em.createQuery(cq).getSingleResult();

		return count > 0;
	}

	@Override
	public boolean exists(String uuid) {
		return campaignFormDataService.exists(uuid);
	}

	@Override
	public CampaignFormDataReferenceDto getReferenceByUuid(String uuid) {
		////System.out.println("sssssssdfgfsdfghjghfhjkssssddsssssssssssssssss");
		return toReferenceDto(campaignFormDataService.getByUuid(uuid));
	}

	@Override
	public CampaignFormDataDto getExistingData(CampaignFormDataCriteria criteria) {
		////System.out.println("ssssssssssssddddddddddddsssssssssssssssssssddsssssssssssssssss");
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<CampaignFormData> cq = cb.createQuery(CampaignFormData.class);
		Root<CampaignFormData> root = cq.from(CampaignFormData.class);

		Predicate filter = CriteriaBuilderHelper.and(cb,
				campaignFormDataService.createCriteriaFilter(criteria, cb, root),
				campaignFormDataService.createUserFilter(cb, cq, root));
		if (filter != null) {
			cq.where(filter);
		}

		cq.orderBy(cb.desc(root.get(CampaignFormData.CHANGE_DATE)));

		return QueryHelper.getFirstResult(em, cq, this::toDto);
	}

	@Override
	public List<CampaignFormDataIndexDto> getIndexList(CampaignFormDataCriteria criteria, Integer first, Integer max,
			List<SortProperty> sortProperties) {

		////System.out.println(criteria.getCampaignFormMeta().getUuid()+"sdddddddddddddfdsyu876456uiuytrertyuyttyukldsssssssssssssssss formValues"+sortProperties.get(1));

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<CampaignFormDataIndexDto> cq = cb.createQuery(CampaignFormDataIndexDto.class);
		Root<CampaignFormData> root = cq.from(CampaignFormData.class);
		Join<CampaignFormData, Campaign> campaignJoin = root.join(CampaignFormData.CAMPAIGN, JoinType.LEFT);
		Join<CampaignFormData, CampaignFormMeta> campaignFormMetaJoin = root.join(CampaignFormData.CAMPAIGN_FORM_META,
				JoinType.LEFT);
		Join<CampaignFormData, Area> areaJoin = root.join(CampaignFormData.AREA, JoinType.LEFT);
		Join<CampaignFormData, Region> regionJoin = root.join(CampaignFormData.REGION, JoinType.LEFT);
		Join<CampaignFormData, District> districtJoin = root.join(CampaignFormData.DISTRICT, JoinType.LEFT);
		Join<CampaignFormData, Community> communityJoin = root.join(CampaignFormData.COMMUNITY, JoinType.LEFT);
		
		cq.multiselect(root.get(CampaignFormData.UUID), campaignJoin.get(Campaign.NAME),
				campaignFormMetaJoin.get(CampaignFormMeta.FORM_NAME),
				criteria.getCampaignFormMeta() != null ? root.get(CampaignFormData.FORM_VALUES)
						: cb.nullLiteral(String.class),
						areaJoin.get(Area.NAME), areaJoin.get(Area.EXTERNAL_ID),
						regionJoin.get(Region.NAME), regionJoin.get(Region.EXTERNAL_ID),
						districtJoin.get(District.NAME), districtJoin.get(District.EXTERNAL_ID),
						communityJoin.get(Community.NAME), communityJoin.get(Community.CLUSTER_NUMBER), communityJoin.get(Community.EXTERNAL_ID),
						root.get(CampaignFormData.FORM_DATE),
				campaignFormMetaJoin.get(CampaignFormMeta.FORM_TYPE));

		
		Predicate filter = CriteriaBuilderHelper.and(cb,
				campaignFormDataService.createCriteriaFilter(criteria, cb, root),
				campaignFormDataService.createUserFilter(cb, cq, root));



		if (filter != null) {
			cq.where(filter);
			//cq.where(typeClause); //can write another filter to remove if archive is true
		} else {
			////System.out.println("DEBUGGER ASDHFUASDFHAS: Filter is null");
		}

		if (sortProperties != null && sortProperties.size() > 0) {
			List<Order> order = new ArrayList<>(sortProperties.size());
			for (SortProperty sortProperty : sortProperties) {
				Expression<?> expression;
				switch (sortProperty.propertyName) {
				case CampaignFormDataIndexDto.UUID:
				case CampaignFormDataIndexDto.FORM_DATE:
					expression = root.get(sortProperty.propertyName);
					break;
				case CampaignFormDataIndexDto.CAMPAIGN:
					expression = campaignJoin.get(Campaign.NAME);
					break;
				case CampaignFormDataIndexDto.FORM:
					expression = campaignFormMetaJoin.get(CampaignFormMeta.FORM_NAME);
					break;
				case CampaignFormDataIndexDto.REGION:
					expression = regionJoin.get(Region.NAME);
					break;
				case CampaignFormDataIndexDto.PCODE:
					expression = regionJoin.get(Region.EXTERNAL_ID);
					break;
				case CampaignFormDataIndexDto.AREA:
					expression = areaJoin.get(Area.NAME);
					break;
				case CampaignFormDataIndexDto.RCODE:
					expression = areaJoin.get(Area.EXTERNAL_ID);
					break;
				case CampaignFormDataIndexDto.DISTRICT:
					expression = districtJoin.get(District.NAME);
					break;
				case CampaignFormDataIndexDto.DCODE:
					expression = districtJoin.get(District.EXTERNAL_ID);
					break;
				case CampaignFormDataIndexDto.COMMUNITY:
					expression = communityJoin.get(Community.NAME);
					break;
				case CampaignFormDataIndexDto.COMMUNITYNUMBER:
					expression = communityJoin.get(Community.CLUSTER_NUMBER);
					break;
				case CampaignFormDataIndexDto.CCODE:
					expression = communityJoin.get(Community.EXTERNAL_ID);
					break;
				case CampaignFormDataIndexDto.FORM_TYPE:
					expression = campaignFormMetaJoin.get(CampaignFormMeta.FORM_TYPE);
					break;
				default:
					throw new IllegalArgumentException(sortProperty.propertyName);
				}
				order.add(sortProperty.ascending ? cb.asc(expression) : cb.desc(expression));
			}
			cq.orderBy(order);
		} else {
			cq.orderBy(cb.desc(root.get(CampaignFormData.CHANGE_DATE)));
		}
		
		//Can add where clause to compare boolean for archive
	//	//System.out.println("DEBUGGER r567ujhgty8ijyu8dfrf  " + SQLExtractor.from(em.createQuery(cq)));
		return QueryHelper.getResultList(em, cq, first, max);
	}

	@Override
	public List<String> getAllActiveUuids() {
		if (userService.getCurrentUser() == null) {
			return Collections.emptyList();
		}
		Date date = new Date(0);

		 List<CampaignFormMeta> allAfter = campaignFormMetaService.getAllAfter(date, userService.getCurrentUser());
			List<CampaignFormMeta> filtered = new ArrayList<>();
			allAfter.removeIf(e -> e.getFormCategory() == null);
			
			for (FormAccess n : userService.getCurrentUser().getFormAccess()) {
				boolean yn = allAfter.stream().filter(e -> !e.getFormCategory().equals(null))
						.filter(ee -> ee.getFormCategory().equals(n)).collect(Collectors.toList()).size() > 0;
				if (yn) {
					filtered.addAll(allAfter.stream().filter(e -> !e.getFormCategory().equals(null))
							.filter(ee -> ee.getFormCategory().equals(n)).collect(Collectors.toList()));
				}
			}
			
			List<CampaignFormData> newlst = campaignFormDataService.getAllActiveAfter(date);
			List<CampaignFormData> filterednewlst = new ArrayList<>();
			for (CampaignFormMeta met : filtered) {
				
				boolean ynn = newlst.stream().filter(eee -> eee.getCampaignFormMeta().getId() != null).filter(ee -> ee.getCampaignFormMeta().getId().equals(met.getId())).collect(Collectors.toList()).size() > 0;
				
				
				if (ynn) {
					
					filterednewlst.addAll(newlst.stream().filter(eee -> eee.getCampaignFormMeta().getId() != null).filter(ee -> ee.getCampaignFormMeta().getId().equals(met.getId())).collect(Collectors.toList()));
				}
			}
			
			 List<CampaignFormData> filterednewlstLst = new ArrayList<>();
				for (Community commm : userService.getCurrentUser().getCommunity()) {
					//System.out.println(">>>>>>>>>>>>getCommunity>>>>>>>>>"+commm.getId());
					boolean ynn = filterednewlst.stream().filter(comf -> comf.getCommunity().getId().equals(commm.getId())).collect(Collectors.toList()).size() > 0;
						
					if (ynn) {
						filterednewlstLst.addAll(filterednewlst.stream().filter(comf -> comf.getCommunity().getId().equals(commm.getId())).filter(comdf -> comdf.getCampaign().isOpenandclose() == true).collect(Collectors.toList()));
					}
				}
				
				
			
			List<String> lstfinal = new ArrayList<>();
			
			 for(CampaignFormData dtfiltere : filterednewlstLst) {
				 //System.out.println(">>>>>>>>>>>>>>>>>_____>>>>>>>>>>>>>>>>>>>"+dtfiltere.getUuid());
				 lstfinal.addAll(campaignFormDataService.getAllActiveUuids().stream().filter(eed -> eed.equals(dtfiltere.getUuid())).collect(Collectors.toList()));
				 }
			 
			 

			 
			 //System.out.println(">>>>>> "+lstfinal.size());

		return lstfinal;
	}

	@Override
	public List<CampaignFormDataDto> getAllActiveAfter(Date date) {
		if (userService.getCurrentUser() == null) {
			return Collections.emptyList();
		}
		
		 List<CampaignFormMeta> allAfter = campaignFormMetaService.getAllAfter(date, userService.getCurrentUser());
			List<CampaignFormMeta> filtered = new ArrayList<>();
			allAfter.removeIf(e -> e.getFormCategory() == null);
			
			for (FormAccess n : userService.getCurrentUser().getFormAccess()) {
				boolean yn = allAfter.stream().filter(e -> !e.getFormCategory().equals(null))
						.filter(ee -> ee.getFormCategory().equals(n)).collect(Collectors.toList()).size() > 0;
				if (yn) {
					filtered.addAll(allAfter.stream().filter(e -> !e.getFormCategory().equals(null))
							.filter(ee -> ee.getFormCategory().equals(n)).collect(Collectors.toList()));
				}
			}
			
			List<CampaignFormData> newlst = campaignFormDataService.getAllActiveAfter(date);
			List<CampaignFormData> filterednewlst = new ArrayList<>();
			for (CampaignFormMeta met : filtered) {
				//System.out.println(">>>>>>>CampaignFormMeta>>>>>>>>>>>"+met.getFormName());
				boolean ynn = newlst.stream().filter(eee -> eee.getCampaignFormMeta().getId() != null).filter(ee -> ee.getCampaignFormMeta().getId().equals(met.getId())).collect(Collectors.toList()).size() > 0;
				
				if (ynn) {
					
					filterednewlst.addAll(newlst.stream().filter(eee -> eee.getCampaignFormMeta().getId() != null).filter(ee -> ee.getCampaignFormMeta().getId().equals(met.getId())).collect(Collectors.toList()));
				}
			}
			
			List<CampaignFormData> filterednewlstLst = new ArrayList<>();
			for (Community commm : userService.getCurrentUser().getCommunity()) {
				//System.out.println(">>>>>>>>>>>>getCommunity>>>>>>>>>"+commm.getId());
				boolean ynn = filterednewlst.stream().filter(comf -> comf.getCommunity().getId().equals(commm.getId())).collect(Collectors.toList()).size() > 0;
					
				if (ynn) {
					filterednewlstLst.addAll(filterednewlst.stream().filter(comf -> comf.getCommunity().getId().equals(commm.getId())).collect(Collectors.toList()));
				}
			}
			
			
			
			//campaignFormDataService.getAllActiveAfter(date)
			
//		return campaignFormDataService.getAllActiveAfter(date).stream().map(c -> convertToDto(c))
//				.collect(Collectors.toList());
				filterednewlstLst.removeIf(ee -> ee.getCampaign().isOpenandclose() == false);
			for (CampaignFormData medt : filterednewlstLst) {
				//System.out.println(medt.getCampaign().isOpenandclose()+ " >>>>>>>>>---------"+medt.getUuid());
					
			}
			
		
			//System.out.println(">>>>>>>>>>>>final items from server>>>>>>>"+filterednewlstLst.size());
		
		return filterednewlstLst.stream().map(c -> convertToDto(c))
				.collect(Collectors.toList());
	}

	@Override
	public List<CampaignFormDataDto> getAllActive() {
		if (userService.getCurrentUser() == null) {
			return Collections.emptyList();
		}
		return campaignFormDataService.getAllActive().stream().map(c -> convertToDto(c)).collect(Collectors.toList());
	}
	
	public List<CampaignDiagramDataDto> getDiagramDataByFieldGroup(CampaignDiagramSeries diagramSeriesTotal, CampaignDiagramSeries diagramSeries,
			CampaignDiagramCriteria campaignDiagramCriteria) {
		List<CampaignDiagramDataDto> resultData = new ArrayList<>();
	
			List<Area> areas = areaService.getAll();
			areas.forEach(areaItem -> {
				Integer population = populationDataFacadeEjb.getAreaPopulation(areaItem.getUuid(),
						diagramSeriesTotal.getPopulationGroup());
				if (population == 0) {
					resultData.add(new CampaignDiagramDataDto(areaItem.getName(), 0, areaItem.getUuid(),
							areaItem.getName(), diagramSeries.getFieldId(), diagramSeries.getFormId(), false));
				} else {
					resultData.add(new CampaignDiagramDataDto(areaItem.getName(), population, areaItem.getUuid(),
							areaItem.getName(), diagramSeries.getFieldId(), diagramSeries.getFormId(), true));
				}
			});
		
		
		
		
		//////System.out.println("t"+resultData);
		
		return resultData;
	}

	public List<CampaignDiagramDataDto> getDiagramDataByAgeGroup(
			CampaignDiagramSeries diagramSeriesTotal, CampaignDiagramSeries diagramSeries,
			CampaignDiagramCriteria campaignDiagramCriteria) {
		List<CampaignDiagramDataDto> resultData = new ArrayList<>();
		final AreaReferenceDto area = campaignDiagramCriteria.getArea();
		final RegionReferenceDto region = campaignDiagramCriteria.getRegion();
		final DistrictReferenceDto district = campaignDiagramCriteria.getDistrict();
		final CampaignJurisdictionLevel grouping = campaignDiagramCriteria.getCampaignJurisdictionLevelGroupBy();
		// final String formTyper = campaignDiagramCriteria.getFormType();

		if (grouping == CampaignJurisdictionLevel.AREA) {
			List<Area> areas = areaService.getAll();
			areas.forEach(areaItem -> {
				Integer population = populationDataFacadeEjb.getAreaPopulation(areaItem.getUuid(),
						diagramSeriesTotal.getPopulationGroup());
			//	//System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>> "+population);
				if (population == 0) {
					resultData.add(new CampaignDiagramDataDto(areaItem.getName(), 0, areaItem.getUuid(),
							areaItem.getName(), diagramSeries.getFieldId(), diagramSeries.getFormId(), false));
				} else {
					resultData.add(new CampaignDiagramDataDto(areaItem.getName(), population, areaItem.getUuid(),
							areaItem.getName(), diagramSeries.getFieldId(), diagramSeries.getFormId(), true));
				}
			});
		} else if (grouping == CampaignJurisdictionLevel.REGION) {
			List<RegionReferenceDto> regions;
			if (area != null)
				regions = regionFacadeEjb.getAllActiveByArea(area.getUuid());
			else
				regions = regionFacadeEjb.getAllActiveAsReference();

			// this should not be needed
//			if (regions.isEmpty()) {
//				resultData.add(
//					new CampaignDiagramDataDto(
//						area.getCaption(),
//						0,
//						area.getUuid(),
//						area.getCaption(),
//						diagramSeries.getFieldId(),
//						diagramSeries.getFormId(),
//						false));
//			} else {
			regions.stream().forEach(regionReferenceDto -> {
				PopulationDataCriteria criteria = new PopulationDataCriteria();
				criteria.sexIsNull(true);
				criteria.region(regionReferenceDto);
				criteria.ageGroup(diagramSeriesTotal.getPopulationGroup());
				List<PopulationDataDto> populationDataDto = populationDataFacadeEjb.getPopulationData(criteria);
				Integer populationSum = 0;
				if (!populationDataDto.isEmpty()) {
					populationSum = populationDataDto.stream().mapToInt(e -> e.getPopulation()).sum();
					resultData.add(new CampaignDiagramDataDto(regionReferenceDto.getCaption(), populationSum,
							regionReferenceDto.getUuid(), regionReferenceDto.getCaption(), diagramSeries.getFieldId(),
							diagramSeries.getFormId(), true));
				} else {
					resultData.add(new CampaignDiagramDataDto(regionReferenceDto.getCaption(), 0,
							regionReferenceDto.getUuid(), regionReferenceDto.getCaption(), diagramSeries.getFieldId(),
							diagramSeries.getFormId(), false));
				}
			});
//			}
		} else if (grouping == CampaignJurisdictionLevel.DISTRICT || Objects.isNull(district)) {

			List<DistrictReferenceDto> districts;
			if (region != null) {
				districts = districtFacadeEjb.getAllActiveByRegion(region.getUuid());
			} else if (area != null) {
				districts = districtFacadeEjb.getAllActiveByArea(area.getUuid());
			} else {
				districts = districtFacadeEjb.getAllActiveAsReference();
			}

			// this should not be needed
//			if (districts.isEmpty()) {
//				resultData.add(
//					new CampaignDiagramDataDto(
//						region.getCaption(),
//						0,
//						region.getUuid(),
//						region.getCaption(),
//						diagramSeries.getFieldId(),
//						diagramSeries.getFormId(),
//						false));
//			} else {
			districts.stream().forEach(districtReferenceDto -> {
				PopulationDataCriteria criteria = new PopulationDataCriteria();
				criteria.sexIsNull(true);
				criteria.district(districtReferenceDto);
				criteria.region(region);
				criteria.ageGroup(diagramSeriesTotal.getPopulationGroup());
				List<PopulationDataDto> populationDataDtoList = populationDataFacadeEjb.getPopulationData(criteria);
				Integer populationSum = 0;
				if (!populationDataDtoList.isEmpty()) {
					populationSum = populationDataDtoList.stream().mapToInt(e -> e.getPopulation()).sum();
					resultData.add(new CampaignDiagramDataDto(districtReferenceDto.getCaption(), populationSum,
							districtReferenceDto.getUuid(), districtReferenceDto.getCaption(),
							diagramSeries.getFieldId(), diagramSeries.getFormId(), true));
				} else {
					resultData.add(new CampaignDiagramDataDto(districtReferenceDto.getCaption(), populationSum,
							districtReferenceDto.getUuid(), districtReferenceDto.getCaption(),
							diagramSeries.getFieldId(), diagramSeries.getFormId(), false));
				}
			});
//			}
		} else if (district != null) {
			resultData.add(new CampaignDiagramDataDto(district.getCaption(), 0, district.getUuid(),
					district.getCaption(), diagramSeries.getFieldId(), diagramSeries.getFormId(), true));
		}
		
		////System.out.println("dddddddddddddddddddddd"+resultData);
		return resultData;
	}

	public List<CampaignDiagramDataDto> getDiagramDataByAgeGroupCard(
			CampaignDiagramSeries diagramSeriesTotal, CampaignDiagramSeries diagramSeries,
			CampaignDiagramCriteria campaignDiagramCriteria) {
		List<CampaignDiagramDataDto> resultData = new ArrayList<>();
		final AreaReferenceDto area = campaignDiagramCriteria.getArea();
		final RegionReferenceDto region = campaignDiagramCriteria.getRegion();
		final DistrictReferenceDto district = campaignDiagramCriteria.getDistrict();
		final CampaignJurisdictionLevel grouping = campaignDiagramCriteria.getCampaignJurisdictionLevelGroupBy();
		// final String formTyper = campaignDiagramCriteria.getFormType();

		if (grouping == CampaignJurisdictionLevel.AREA) {
			List<Area> areas = areaService.getAll();
			areas.forEach(areaItem -> {
				Integer population = populationDataFacadeEjb.getAreaPopulationParent(areaItem.getUuid(),
						diagramSeriesTotal.getPopulationGroup());
				
		//		//System.out.println(diagramSeriesTotal.getPopulationGroup()+">>>>>>>>>>>>>>>YEAH - population = "+population);
				
				if (population == 0) {
					resultData.add(new CampaignDiagramDataDto(areaItem.getName(), 0, areaItem.getUuid(),
							areaItem.getName(), diagramSeries.getFieldId(), diagramSeries.getFormId(), false));
				} else {
					resultData.add(new CampaignDiagramDataDto(areaItem.getName(), population, areaItem.getUuid(),
							areaItem.getName(), diagramSeries.getFieldId(), diagramSeries.getFormId(), true));
				}
			});
		} else if (grouping == CampaignJurisdictionLevel.REGION) {
			List<RegionReferenceDto> regions;
			if (area != null)
				regions = regionFacadeEjb.getAllActiveByArea(area.getUuid());
			else
				regions = regionFacadeEjb.getAllActiveAsReference();

			// this should not be needed
//			if (regions.isEmpty()) {
//				resultData.add(
//					new CampaignDiagramDataDto(
//						area.getCaption(),
//						0,
//						area.getUuid(),
//						area.getCaption(),
//						diagramSeries.getFieldId(),
//						diagramSeries.getFormId(),
//						false));
//			} else {
			regions.stream().forEach(regionReferenceDto -> {
				PopulationDataCriteria criteria = new PopulationDataCriteria();
				criteria.sexIsNull(true);
				criteria.region(regionReferenceDto);
				criteria.ageGroup(diagramSeriesTotal.getPopulationGroup());
				List<PopulationDataDto> populationDataDto = populationDataFacadeEjb.getPopulationData(criteria);
				Integer populationSum = 0;
				if (!populationDataDto.isEmpty()) {
					populationSum = populationDataDto.stream().mapToInt(e -> e.getPopulation()).sum();
					resultData.add(new CampaignDiagramDataDto(regionReferenceDto.getCaption(), populationSum,
							regionReferenceDto.getUuid(), regionReferenceDto.getCaption(), diagramSeries.getFieldId(),
							diagramSeries.getFormId(), true));
				} else {
					resultData.add(new CampaignDiagramDataDto(regionReferenceDto.getCaption(), 0,
							regionReferenceDto.getUuid(), regionReferenceDto.getCaption(), diagramSeries.getFieldId(),
							diagramSeries.getFormId(), false));
				}
			});
//			}
		} else if (grouping == CampaignJurisdictionLevel.DISTRICT || Objects.isNull(district)) {

			List<DistrictReferenceDto> districts;
			if (region != null) {
				districts = districtFacadeEjb.getAllActiveByRegion(region.getUuid());
			} else if (area != null) {
				districts = districtFacadeEjb.getAllActiveByArea(area.getUuid());
			} else {
				districts = districtFacadeEjb.getAllActiveAsReference();
			}

			// this should not be needed
//			if (districts.isEmpty()) {
//				resultData.add(
//					new CampaignDiagramDataDto(
//						region.getCaption(),
//						0,
//						region.getUuid(),
//						region.getCaption(),
//						diagramSeries.getFieldId(),
//						diagramSeries.getFormId(),
//						false));
//			} else {
			districts.stream().forEach(districtReferenceDto -> {
				PopulationDataCriteria criteria = new PopulationDataCriteria();
				criteria.sexIsNull(true);
				criteria.district(districtReferenceDto);
				criteria.region(region);
				criteria.ageGroup(diagramSeriesTotal.getPopulationGroup());
				List<PopulationDataDto> populationDataDtoList = populationDataFacadeEjb.getPopulationData(criteria);
				Integer populationSum = 0;
				if (!populationDataDtoList.isEmpty()) {
					populationSum = populationDataDtoList.stream().mapToInt(e -> e.getPopulation()).sum();
					resultData.add(new CampaignDiagramDataDto(districtReferenceDto.getCaption(), populationSum,
							districtReferenceDto.getUuid(), districtReferenceDto.getCaption(),
							diagramSeries.getFieldId(), diagramSeries.getFormId(), true));
				} else {
					resultData.add(new CampaignDiagramDataDto(districtReferenceDto.getCaption(), populationSum,
							districtReferenceDto.getUuid(), districtReferenceDto.getCaption(),
							diagramSeries.getFieldId(), diagramSeries.getFormId(), false));
				}
			});
//			}
		} else if (district != null) {
			resultData.add(new CampaignDiagramDataDto(district.getCaption(), 0, district.getUuid(),
					district.getCaption(), diagramSeries.getFieldId(), diagramSeries.getFormId(), true));
		}
		
	//	//System.out.println("dddddddddddddddddddddd"+resultData);
		return resultData;
	}

	
	

	@Override
	public List<CampaignDiagramDataDto> getDiagramData(List<CampaignDiagramSeries> diagramSeries,
			CampaignDiagramCriteria campaignDiagramCriteria) {
		List<CampaignDiagramDataDto> resultData = new ArrayList<>();
		final AreaReferenceDto area = campaignDiagramCriteria.getArea();
		final RegionReferenceDto region = campaignDiagramCriteria.getRegion();
		final DistrictReferenceDto district = campaignDiagramCriteria.getDistrict();
		final CampaignReferenceDto campaign = campaignDiagramCriteria.getCampaign();

		for (CampaignDiagramSeries series : diagramSeries) {
			//@formatter:off

				final String areaFilter = area != null ? " AND " + Area.TABLE_NAME + "." + Area.UUID + " = :areaUuid" : "";
				final String regionFilter = region != null ? " AND " + CampaignFormData.REGION + "." + Region.UUID + " = :regionUuid" : "";
				final String districtFilter = district != null ? " AND " + CampaignFormData.DISTRICT + "." + District.UUID + " = :districtUuid" : "";
				final String campaignFilter = campaign != null ? " AND " + Campaign.TABLE_NAME + "." + Campaign.UUID + " = :campaignUuid" : "";
				//@formatter:on

			// SELECT
			StringBuilder selectBuilder = new StringBuilder("SELECT ").append(CampaignFormMeta.TABLE_NAME).append(".")
					.append(CampaignFormMeta.UUID).append(" as formUuid,").append(CampaignFormMeta.TABLE_NAME)
					.append(".").append(CampaignFormMeta.FORM_ID).append(" as formId");

			if (series.getFieldId() != null) {
				
				selectBuilder.append(", jsonData->>'")
				.append(CampaignFormDataEntry.ID)
				.append("' as fieldId, jsonMeta->>'")
				.append(CampaignFormElement.CAPTION)
				.append("' as fieldCaption,");
				
				if(series.getReferenceValue() != null) {
					////System.out.println("+==+ "+series.getReferenceValue());
					selectBuilder.append("sum (CASE WHEN ").append("((jsonData->>'")
					.append(CampaignFormDataEntry.VALUE)
					.append("') = '").append(series.getReferenceValue())
					.append("') THEN 1 ELSE 0 END)"); 
				} else {
						
					////System.out.println("+==+ "+series.getReferenceValue());
				selectBuilder.append(" CASE WHEN ((jsonMeta ->> '")
						.append(CampaignFormElement.TYPE).append("') = '")
						.append(CampaignFormElementType.NUMBER.toString() + "') OR")

						.append("((jsonMeta ->> '").append(CampaignFormElement.TYPE).append("') = '")
						.append(CampaignFormElementType.DECIMAL.toString() + "') OR")

						.append("((jsonMeta ->> '").append(CampaignFormElement.TYPE).append("') = '")
						.append(CampaignFormElementType.RANGE.toString() + "')");

				if (series.getAverageDefault() != null && "true".equals(series.getAverageDefault())) {
					selectBuilder.append(" THEN avg(cast_number(jsonData->>'");
				} else {
					selectBuilder.append(" THEN sum(cast_number(jsonData->>'");
				}
						
				selectBuilder.append(CampaignFormDataEntry.VALUE)
						.append("', 0)) ELSE sum(CASE WHEN(jsonData->>'").append(CampaignFormDataEntry.VALUE)
					.append("') = '")//.append(series.getReferenceValue())
					.append("' THEN 0 ELSE 1 END) END");
					
				}
						selectBuilder.append(" as sumValue,");
				
						//.append("THEN round(SUM (CAST (jsonData->>'").append(CampaignFormDataEntry.VALUE)
						//.append("' AS NUMERIC)), 2) ELSE sum(CASE WHEN(jsonData->>'").append(CampaignFormDataEntry.VALUE)
						//.append("') = '").append(series.getReferenceValue())
						//.append("' THEN 1 ELSE 0 END) END as sumValue,");
			}
		else {
				selectBuilder.append(", null as fieldId, null as fieldCaption, count(formId) as sumValue,");
			}

			final String jurisdictionGrouping;
			switch (campaignDiagramCriteria.getCampaignJurisdictionLevelGroupBy()) {
			case REGION:
				appendInfrastructureSelection(selectBuilder, Region.TABLE_NAME, Region.NAME);
				break;
			case DISTRICT:
				appendInfrastructureSelection(selectBuilder, District.TABLE_NAME, District.NAME);
				break;
			case COMMUNITY:
				appendInfrastructureSelection(selectBuilder, Community.TABLE_NAME, Community.NAME);
				break;
			case AREA:
			default:
				appendInfrastructureSelection(selectBuilder, Area.TABLE_NAME, Area.NAME);
			}

			// JOINS
			StringBuilder joinBuilder = new StringBuilder(" LEFT JOIN ").append(CampaignFormMeta.TABLE_NAME)
					.append(" ON ").append(CampaignFormData.CAMPAIGN_FORM_META).append("_id = ")
					.append(CampaignFormMeta.TABLE_NAME).append(".").append(CampaignFormMeta.ID).append(" LEFT JOIN ")
					.append(Region.TABLE_NAME).append(" ON ").append(CampaignFormData.REGION).append("_id =")
					.append(Region.TABLE_NAME).append(".").append(Region.ID).append(" LEFT JOIN ")
					.append(Area.TABLE_NAME).append(" ON ").append(CampaignFormData.TABLE_NAME).append(".")
					.append(Region.AREA).append("_id = ").append(Area.TABLE_NAME).append(".").append(Area.ID)
					.append(" LEFT JOIN ").append(District.TABLE_NAME).append(" ON ").append(CampaignFormData.DISTRICT)
					.append("_id = ").append(District.TABLE_NAME).append(".").append(District.ID).append(" LEFT JOIN ")
					.append(Community.TABLE_NAME).append(" ON ").append(CampaignFormData.COMMUNITY).append("_id = ")
					.append(Community.TABLE_NAME).append(".").append(Community.ID).append(" LEFT JOIN ")
					.append(Campaign.TABLE_NAME).append(" ON ").append(CampaignFormData.CAMPAIGN).append("_id = ")
					.append(Campaign.TABLE_NAME).append(".").append(Campaign.ID);

			if (series.getFieldId() != null) {
				joinBuilder.append(", json_array_elements(").append(CampaignFormData.FORM_VALUES)
						.append(") as jsonData, json_array_elements(").append(CampaignFormMeta.CAMPAIGN_FORM_ELEMENTS)
						.append(") as jsonMeta");
			}

			// WHERE
			StringBuilder whereBuilder = new StringBuilder(" WHERE ").append(CampaignFormMeta.TABLE_NAME).append(".")
					.append(CampaignFormMeta.FORM_ID).append(" = :campaignFormMetaId");

			String cds = "";

			if (series.getFieldId() != null) {
				whereBuilder.append(" AND jsonData->>'").append(CampaignFormDataEntry.ID)
						.append("' = :campaignFormDataId").append(" AND jsonData->>'")
						.append(CampaignFormDataEntry.VALUE).append("' IS NOT NULL AND jsonData->>'")
						.append(CampaignFormDataEntry.ID).append("' = jsonMeta->>'").append(CampaignFormElement.ID)
						.append("'");
			}

			whereBuilder.append(areaFilter).append(regionFilter).append(districtFilter).append(campaignFilter);// .append(campaignPhaseFilter);

			// GROUP BY
			StringBuilder groupByBuilder = new StringBuilder(" GROUP BY ").append(CampaignFormMeta.TABLE_NAME)
					.append(".").append(CampaignFormMeta.UUID).append(",").append(CampaignFormMeta.TABLE_NAME)
					.append(".").append(CampaignFormMeta.FORM_ID);

			if (series.getFieldId() != null) {
				groupByBuilder.append(", jsonData->>'").append(CampaignFormDataEntry.ID).append("', jsonMeta->>'")
						.append(CampaignFormElement.CAPTION).append("', jsonMeta->>'").append(CampaignFormElement.TYPE)
						.append("'");
			}

			switch (campaignDiagramCriteria.getCampaignJurisdictionLevelGroupBy()) {
			case REGION:
				jurisdictionGrouping = ", " + Region.TABLE_NAME + "." + Region.UUID + ", " + Region.TABLE_NAME + "."
						+ Region.NAME;
				break;
			case DISTRICT:
				jurisdictionGrouping = ", " + District.TABLE_NAME + "." + District.UUID + ", " + District.TABLE_NAME
						+ "." + District.NAME;
				break;
			case COMMUNITY:
				jurisdictionGrouping = ", " + Community.TABLE_NAME + "." + Community.UUID + ", " + Community.TABLE_NAME
						+ "." + Community.NAME;
				break;
			case AREA:
			default:
				jurisdictionGrouping = ", " + Area.TABLE_NAME + "." + Area.UUID + ", " + Area.TABLE_NAME + "."
						+ Area.NAME;
			}

			groupByBuilder.append(jurisdictionGrouping);

		//	//System.out.println("getDataDiagram "+selectBuilder.toString() + " FROM " + CampaignFormData.TABLE_NAME +" "+ joinBuilder +" "+ whereBuilder +" "+ groupByBuilder);
			
			// +selectBuilder.toString() + " FROM " + CampaignFormData.TABLE_NAME +
			// joinBuilder

			//@formatter:off
			Query seriesDataQuery = em.createNativeQuery(
					selectBuilder.toString() + " FROM " + CampaignFormData.TABLE_NAME + joinBuilder + whereBuilder + groupByBuilder);
			//@formatter:on

			seriesDataQuery.setParameter("campaignFormMetaId", series.getFormId());
			if (area != null) {
				seriesDataQuery.setParameter("areaUuid", area.getUuid());
			}
			if (region != null) {
				seriesDataQuery.setParameter("regionUuid", region.getUuid());
			}
			if (district != null) {
				seriesDataQuery.setParameter("districtUuid", district.getUuid());
			}
			if (campaign != null) {
				seriesDataQuery.setParameter("campaignUuid", campaign.getUuid());
			}

			if (series.getFieldId() != null) {
				seriesDataQuery.setParameter("campaignFormDataId", series.getFieldId());
			}
			
			@SuppressWarnings("unchecked")
			List<Object[]> resultList = seriesDataQuery.getResultList(); 
			
			

			resultData.addAll(resultList.stream()
					.map((result) -> new CampaignDiagramDataDto((String) result[0], (String) result[1],
							(String) result[2], (String) result[3], result[4].toString(), (String) result[5],
							(String) result[6],
							// (String) result[7],
							series.getStack()))
					.collect(Collectors.toList()));
		}
		////System.out.println("resultData - "+ resultData.toString());//SQLExtractor.from(seriesDataQuery));
		return resultData;
	}

	

	
	@Override
	public List<CampaignDiagramDataDto> getDiagramDataByGroups(List<CampaignDiagramSeries> diagramSeries,
			CampaignDiagramCriteria campaignDiagramCriteria) { 
		List<CampaignDiagramDataDto> resultData = new ArrayList<>();
		final AreaReferenceDto area = campaignDiagramCriteria.getArea();
		final RegionReferenceDto region = campaignDiagramCriteria.getRegion();
		final DistrictReferenceDto district = campaignDiagramCriteria.getDistrict();
		final CampaignReferenceDto campaign = campaignDiagramCriteria.getCampaign();

		/*
		 * String formType = campaignDiagramCriteria.getFormType(); 
		 * 
		 * if ("all phases".equals(formType)) { formType = null; }
		 */

		for (CampaignDiagramSeries series : diagramSeries) {
			//@formatter:off

				final String areaFilter = area != null ? " AND " + Area.TABLE_NAME + "." + Area.UUID + " = :areaUuid" : "";
				final String regionFilter = region != null ? " AND " + CampaignFormData.REGION + "." + Region.UUID + " = :regionUuid" : "";
				final String districtFilter = district != null ? " AND " + CampaignFormData.DISTRICT + "." + District.UUID + " = :districtUuid" : "";
				final String campaignFilter = campaign != null ? " AND " + Campaign.TABLE_NAME + "." + Campaign.UUID + " = :campaignUuid" : "";
			//	final String campaignPhaseFilter = formType != null ? " AND " + CampaignFormData.CAMPAIGN_FORM_META + ".formtype = '"+formType+"'" : "";
				//@formatter:on

				StringBuilder selectBuilder = new StringBuilder("SELECT ").append(CampaignFormMeta.TABLE_NAME).append(".")
						.append(CampaignFormMeta.UUID).append(" as formUuid,").append(CampaignFormMeta.TABLE_NAME)
						.append(".").append(CampaignFormMeta.FORM_ID).append(" as formId");

				if (series.getFieldId() != null) {
					
					selectBuilder.append(", jsonData->>'")
					.append(CampaignFormDataEntry.ID)
					.append("' as fieldId, jsonMeta->>'")
					.append(CampaignFormElement.CAPTION)
					.append("' as fieldCaption,");
					
					if(series.getReferenceValue() != null) {
						selectBuilder.append("sum (CASE WHEN ").append("((jsonData->>'")
						.append(CampaignFormDataEntry.VALUE)
						.append("') = '").append(series.getReferenceValue())
						.append("') THEN 1 ELSE 0 END)"); 
					} else {
					selectBuilder.append(" CASE WHEN ((jsonMeta ->> '")
							.append(CampaignFormElement.TYPE).append("') = '")
							.append(CampaignFormElementType.NUMBER.toString() + "') OR")

							.append("((jsonMeta ->> '").append(CampaignFormElement.TYPE).append("') = '")
							.append(CampaignFormElementType.DECIMAL.toString() + "') OR")

							.append("((jsonMeta ->> '").append(CampaignFormElement.TYPE).append("') = '")
							.append(CampaignFormElementType.RANGE.toString() + "')");

					if(series.getAverageDefault() != null && "true".equals(series.getAverageDefault())){
							selectBuilder.append(" THEN avg(cast_number(jsonData->>'");
					}else{
						selectBuilder.append(" THEN sum(cast_number(jsonData->>'");
					}
					selectBuilder.append(CampaignFormDataEntry.VALUE)
							.append("', 0)) ELSE sum(CASE WHEN(jsonData->>'").append(CampaignFormDataEntry.VALUE)
						.append("') = '")//.append(series.getReferenceValue())
						.append("' THEN 0 ELSE 1 END) END");
						
					}
							selectBuilder.append(" as sumValue");
			} else {
				selectBuilder.append(", null as fieldId, null as fieldCaption, count(formId) as sumValue");
			}
/*
			final String jurisdictionGrouping;
			switch (campaignDiagramCriteria.getCampaignJurisdictionLevelGroupBy()) {
			case REGION:
			//	appendInfrastructureSelection(selectBuilder, Region.TABLE_NAME, Region.NAME);
				break;
			case DISTRICT:
			//	appendInfrastructureSelection(selectBuilder, District.TABLE_NAME, District.NAME);
				break;
			case COMMUNITY:
			//	appendInfrastructureSelection(selectBuilder, Community.TABLE_NAME, Community.NAME);
				break;
			case AREA:
			default:
			//	appendInfrastructureSelection(selectBuilder, Area.TABLE_NAME, Area.NAME);
			}
*/
			// JOINS
			StringBuilder joinBuilder = new StringBuilder(" LEFT JOIN ").append(CampaignFormMeta.TABLE_NAME)
					.append(" ON ").append(CampaignFormData.CAMPAIGN_FORM_META).append("_id = ")
					.append(CampaignFormMeta.TABLE_NAME).append(".").append(CampaignFormMeta.ID).append(" LEFT JOIN ")
					.append(Region.TABLE_NAME).append(" ON ").append(CampaignFormData.REGION).append("_id =")
					.append(Region.TABLE_NAME).append(".").append(Region.ID).append(" LEFT JOIN ")
					.append(Area.TABLE_NAME).append(" ON ").append(CampaignFormData.TABLE_NAME).append(".")
					.append(Region.AREA).append("_id = ").append(Area.TABLE_NAME).append(".").append(Area.ID)
					.append(" LEFT JOIN ").append(District.TABLE_NAME).append(" ON ").append(CampaignFormData.DISTRICT)
					.append("_id = ").append(District.TABLE_NAME).append(".").append(District.ID).append(" LEFT JOIN ")
					.append(Community.TABLE_NAME).append(" ON ").append(CampaignFormData.COMMUNITY).append("_id = ")
					.append(Community.TABLE_NAME).append(".").append(Community.ID).append(" LEFT JOIN ")
					.append(Campaign.TABLE_NAME).append(" ON ").append(CampaignFormData.CAMPAIGN).append("_id = ")
					.append(Campaign.TABLE_NAME).append(".").append(Campaign.ID);

			if (series.getFieldId() != null) {
				joinBuilder.append(", json_array_elements(").append(CampaignFormData.FORM_VALUES)
						.append(") as jsonData, json_array_elements(").append(CampaignFormMeta.CAMPAIGN_FORM_ELEMENTS)
						.append(") as jsonMeta");
			}

			// WHERE
			StringBuilder whereBuilder = new StringBuilder(" WHERE ").append(CampaignFormMeta.TABLE_NAME).append(".")
					.append(CampaignFormMeta.FORM_ID).append(" = :campaignFormMetaId");

			String cds = "";

			if (series.getFieldId() != null) {
				whereBuilder.append(" AND jsonData->>'").append(CampaignFormDataEntry.ID)
						.append("' = :campaignFormDataId").append(" AND jsonData->>'")
						.append(CampaignFormDataEntry.VALUE).append("' IS NOT NULL AND jsonData->>'")
						.append(CampaignFormDataEntry.ID).append("' = jsonMeta->>'").append(CampaignFormElement.ID)
						.append("'");
			}

			whereBuilder.append(areaFilter).append(regionFilter).append(districtFilter).append(campaignFilter);// .append(campaignPhaseFilter);

			// GROUP BY
			StringBuilder groupByBuilder = new StringBuilder(" GROUP BY ").append(CampaignFormMeta.TABLE_NAME)
					.append(".").append(CampaignFormMeta.UUID).append(",").append(CampaignFormMeta.TABLE_NAME)
					.append(".").append(CampaignFormMeta.FORM_ID);

			if (series.getFieldId() != null) {
				groupByBuilder.append(", jsonData->>'").append(CampaignFormDataEntry.ID).append("', jsonMeta->>'")
						.append(CampaignFormElement.CAPTION).append("', jsonMeta->>'").append(CampaignFormElement.TYPE)
						.append("', campaigns.name");
			}


			//@formatter:off
			Query seriesDataQuery = em.createNativeQuery(
					selectBuilder.toString() + " FROM " + CampaignFormData.TABLE_NAME + joinBuilder + whereBuilder + groupByBuilder);
			//@formatter:on

			seriesDataQuery.setParameter("campaignFormMetaId", series.getFormId());
			if (area != null) {
				seriesDataQuery.setParameter("areaUuid", area.getUuid());
			}
			if (region != null) {
				seriesDataQuery.setParameter("regionUuid", region.getUuid());
			}
			if (district != null) {
				seriesDataQuery.setParameter("districtUuid", district.getUuid());
			}
			if (campaign != null) {
				seriesDataQuery.setParameter("campaignUuid", campaign.getUuid());
			}

			if (series.getFieldId() != null) {
				seriesDataQuery.setParameter("campaignFormDataId", series.getFieldId());
			}
			

	//		//System.out.println("getDiagramDataByGroups"+selectBuilder.toString() + " FROM " + CampaignFormData.TABLE_NAME +" "+ joinBuilder +" "+ whereBuilder +" "+ groupByBuilder);
		
			@SuppressWarnings("unchecked")
			List<Object[]> resultList = seriesDataQuery.getResultList(); 
			
			

			resultData.addAll(resultList.stream()
					.map((result) -> new CampaignDiagramDataDto((String) result[0], (String) result[1],
							(String) result[2], (String) result[3], result[4].toString(),// (String) result[5],
							//(String) result[6],
							// (String) result[7],
							series.getStack()))
					.collect(Collectors.toList()));
		}
		return resultData;
	}

	
	
	
	private void appendInfrastructureSelection(StringBuilder sb, String tableNameField, String nameField) {
		sb.append(tableNameField).append(".").append(AbstractDomainObject.UUID).append(", ").append(tableNameField)
				.append(".").append(nameField);
	}

	@Override
	public long count(CampaignFormDataCriteria criteria) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<CampaignFormData> root = cq.from(CampaignFormData.class);

		Predicate filter = CriteriaBuilderHelper.and(cb,
				campaignFormDataService.createCriteriaFilter(criteria, cb, root),
				campaignFormDataService.createUserFilter(cb, cq, root));
		if (filter != null) {
			cq.where(filter);
		}

		cq.select(cb.count(root));
		return em.createQuery(cq).getSingleResult();
	}

	@Override
	public void overwriteCampaignFormData(CampaignFormDataDto existingData, CampaignFormDataDto newData) {
		DtoHelper.copyDtoValues(existingData, newData, true);
		saveCampaignFormData(existingData);
	}

	private CampaignFormDataReferenceDto toReferenceDto(CampaignFormData source) {
		if (source == null) {
			return null;
		}

		return source.toReference();
	}

	@LocalBean
	@Stateless
	public static class CampaignFormDataFacadeEjbLocal extends CampaignFormDataFacadeEjb {
	}

	@Override
	public List<CampaignFormDataDto> getCampaignFormData(String campaignformuuid, String formuuid) {
		// get the campaign
		Campaign xxxxx = campaignService.getByUuid(campaignformuuid);
		Long metaid = xxxxx.getId();
		// get the form
		CampaignFormMeta yyyyyy = campaignFormMetaService.getByUuid(formuuid);
		Long formid = yyyyyy.getId();
		
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<CampaignFormData> cq = cb.createQuery(CampaignFormData.class);
		Root<CampaignFormData> from = cq.from(CampaignFormData.class);

		cq.where(
				cb.and(
				cb.equal(from.get(CampaignFormData.CAMPAIGN_FORM_META), formid),
				cb.equal(from.get(CampaignFormData.CAMPAIGN), metaid)
				));

		CriteriaQuery<CampaignFormData> select = ((CriteriaQuery<CampaignFormData>) cq).select(from);
		TypedQuery<CampaignFormData> tq = em.createQuery(select);
		List<CampaignFormData> list = tq.getResultList();
		
		return list.stream().map(c -> convertToDto(c)).collect(Collectors.toList()); //multiselect(
	}

	@Override
	public List<MapCampaignDataDto> getCampaignDataforMaps() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MapCampaignDataDto> cq = cb.createQuery(MapCampaignDataDto.class);
		Root<CampaignFormData> caze = cq.from(CampaignFormData.class);

		
		

		List<MapCampaignDataDto> result;
		
			cq.multiselect(
				caze.get(CampaignFormData.UUID),
				caze.get(CampaignFormData.LAT),
				caze.get(CampaignFormData.LON));
				

			result = em.createQuery(cq).getResultList();
		
		return result;
	}
	
	@Override
	public String getByClusterDropDown(CommunityReferenceDto community, CampaignFormMetaDto campaignForm,
			CampaignDto campaign) {
//System.out.println(community.getUuid());
//System.out.println(campaignForm.getUuid());	
//System.out.println(campaign.getUuid());
		
		String query = "select cb.uuid from campaignformdata cb left join community cm on cb.community_id = cm.id \r\n"
				+ "left join campaignformmeta ff on cb.campaignformmeta_id = ff.id left join campaigns gn on cb.campaign_id = gn.id\r\n"
				+ "where cm.uuid = '" + community.getUuid() + "' and ff.uuid = '" + campaignForm.getUuid()
				+ "' and gn.uuid = '" + campaign.getUuid() + "' limit 1";
		Query poquery = em.createNativeQuery(query);
		try {
			return (String) poquery.getSingleResult();
		} catch (NoResultException e) {
			return "nul";
		}
		
	}

}
