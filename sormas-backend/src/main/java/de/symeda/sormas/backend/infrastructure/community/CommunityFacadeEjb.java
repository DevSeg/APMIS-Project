package de.symeda.sormas.backend.infrastructure.community;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.vladmihalcea.hibernate.type.util.SQLExtractor;

import de.symeda.sormas.api.ErrorStatusEnum;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.campaign.CampaignPhase;
import de.symeda.sormas.api.common.Page;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.community.CommunityCriteriaNew;
import de.symeda.sormas.api.infrastructure.community.CommunityDto;
import de.symeda.sormas.api.infrastructure.community.CommunityFacade;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.report.CommunityUserReportModelDto;
import de.symeda.sormas.api.user.FormAccess;
import de.symeda.sormas.api.utils.SortProperty;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.backend.common.CriteriaBuilderHelper;
import de.symeda.sormas.backend.feature.FeatureConfigurationFacadeEjb.FeatureConfigurationFacadeEjbLocal;
import de.symeda.sormas.backend.infrastructure.AbstractInfrastructureEjb;
import de.symeda.sormas.backend.infrastructure.area.Area;
import de.symeda.sormas.backend.infrastructure.district.District;
import de.symeda.sormas.backend.infrastructure.district.DistrictFacadeEjb;
import de.symeda.sormas.backend.infrastructure.district.DistrictService;
import de.symeda.sormas.backend.infrastructure.facility.Facility;
import de.symeda.sormas.backend.infrastructure.region.Region;
import de.symeda.sormas.backend.infrastructure.region.RegionFacadeEjb;
import de.symeda.sormas.backend.user.User;
import de.symeda.sormas.backend.user.UserFacadeEjb;
import de.symeda.sormas.backend.user.UserService;
import de.symeda.sormas.backend.util.DtoHelper;
import de.symeda.sormas.backend.util.ModelConstants;
import de.symeda.sormas.backend.util.QueryHelper;

@Stateless(name = "CommunityFacade")
public class CommunityFacadeEjb extends AbstractInfrastructureEjb<Community, CommunityService> implements CommunityFacade {
	
	private FormAccess frmsAccess;
	private ErrorStatusEnum errorStatusEnum;
	


	@PersistenceContext(unitName = ModelConstants.PERSISTENCE_UNIT_NAME)
	private EntityManager em;

	@EJB
	private UserService userService;
	@EJB
	private DistrictService districtService;

	public CommunityFacadeEjb() {
	}

	@Inject
	protected CommunityFacadeEjb(CommunityService service, FeatureConfigurationFacadeEjbLocal featureConfiguration) {
		super(service, featureConfiguration);
	}

	@Override
	public List<CommunityReferenceDto> getAllActiveByDistrict(String districtUuid) {

		District district = districtService.getByUuid(districtUuid);
		return district.getCommunities().stream().filter(c -> !c.isArchived()).map(CommunityFacadeEjb::toReferenceDto).collect(Collectors.toList());
	}

	@Override
	public List<CommunityDto> getAllAfter(Date date) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<CommunityDto> cq = cb.createQuery(CommunityDto.class);
		Root<Community> community = cq.from(Community.class);

		selectDtoFields(cq, community);

		Predicate filter = service.createChangeDateFilter(cb, community, date);

		if (filter != null) {
			cq.where(filter);
		}

		return em.createQuery(cq).getResultList();
	}

	// Need to be in the same order as in the constructor
	private void selectDtoFields(CriteriaQuery<CommunityDto> cq, Root<Community> root) {

		Join<Community, District> district = root.join(Community.DISTRICT, JoinType.LEFT);
		Join<District, Region> region = district.join(District.REGION, JoinType.LEFT);

		cq.multiselect(
			root.get(Community.CREATION_DATE),
			root.get(Community.CHANGE_DATE),
			root.get(Community.UUID),
			root.get(Community.ARCHIVED),
			root.get(Community.NAME),
			root.get(Community.GROWTH_RATE),
			region.get(Region.UUID),
			region.get(Region.NAME),
			region.get(Region.EXTERNAL_ID),
			district.get(District.UUID),
			district.get(District.NAME),
			district.get(District.EXTERNAL_ID),
			root.get(Community.EXTERNAL_ID),
			root.get(Community.CLUSTER_NUMBER));
	}
	
	

	@Override
	public long countReportGrid(CommunityCriteriaNew criteria, FormAccess formacc) {

		//System.out.println("zzzzzzzyyyyyyyejsrtykjstykstykstukszzzzzzzzzzzzzzzzzzzzzzzzz ");
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<Community> root = cq.from(Community.class);

		
		Predicate filter = null;
	//	if (criteria != null) {
			
			filter = service.buildCriteriaFilter(criteria, cb, root);
		//}else {
		//	criteria.fromUrlParams("area=W5R34K-APYPCA-4GZXDO-IVJWKGIM");
	//		filter = service.buildCriteriaFilter(criteria, cb, root);
		//}

		if (filter != null) {
			cq.where(filter);
		}

		cq.select(cb.count(root));
		System.out.println("DEBUGGER r567ujhgty8ijyu8dfrf COUNTERs  " + SQLExtractor.from(em.createQuery(cq)));
		return em.createQuery(cq).getSingleResult();
	}
	

	@Override
	public List<CommunityUserReportModelDto> getAllActiveCommunitytoRerence(CommunityCriteriaNew criteria, Integer first, Integer max, List<SortProperty> sortProperties, FormAccess formacc) {
	System.out.println(criteria.getErrorStatusEnum() + "22222222222222222222222222222 1.0.3 222222222222222222222222222222222222222222222222222222222222 "+criteria.getArea().getUuid());
		frmsAccess = formacc;
		errorStatusEnum = criteria.getErrorStatusEnum();
		
		if(max > 47483647) {
			first = 1;
			max = 100;
			}
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Community> cq = cb.createQuery(Community.class);
			Root<Community> community = cq.from(Community.class);
			Join<Community, District> district = community.join(Community.DISTRICT, JoinType.LEFT);
			Join<District, Region> region = district.join(District.REGION, JoinType.LEFT);
	
					
			Predicate filter = null;
			if (criteria != null) {
				filter = service.buildCriteriaFilter(criteria, cb, community);
			}else {
				filter = cb.equal(community.get(Community.ARCHIVED), false);
			}

			if (filter != null) {
				cq.where(filter);
			}
			
			cq.orderBy(cb.asc(region.get(Region.NAME)), cb.asc(district.get(District.NAME)), cb.asc(community.get(Community.NAME)));
			
			cq.select(community);

			System.out.println("DEBUGGER r567ujhgty8isdfasjyu8dfrf  " + SQLExtractor.from(em.createQuery(cq)));
			

			return QueryHelper.getResultList(em, cq, 1, 20, this::toDtoList);//.stream().filter(e -> e.getMessage() != "Correctly assigned").collect(Collectors.toList());
}
	
	
	@Override
	public List<CommunityUserReportModelDto> getAllActiveCommunitytoRerenceFlow(CommunityCriteriaNew criteria, Integer first, Integer max, List<SortProperty> sortProperties, FormAccess formacc) {
		frmsAccess = formacc;
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Community> cq = cb.createQuery(Community.class);
			Root<Community> community = cq.from(Community.class);
			Join<Community, District> district = community.join(Community.DISTRICT, JoinType.LEFT);
			Join<District, Region> region = district.join(District.REGION, JoinType.LEFT);
	
					
			Predicate filter = null;
			if (criteria != null) {
				filter = service.buildCriteriaFilter(criteria, cb, community);
			}else {
				filter = cb.equal(community.get(Community.ARCHIVED), false);
			}

			if (filter != null) {
				cq.where(filter);
			}
			
			cq.orderBy(cb.asc(region.get(Region.NAME)), cb.asc(district.get(District.NAME)), cb.asc(community.get(Community.NAME)));
			
			cq.select(community);

			System.out.println("DEBUGGER r567ujhgty8isdfasjyu8dfrf  " + SQLExtractor.from(em.createQuery(cq)));
		
			return QueryHelper.getResultList(em, cq, 1, 20, this::toDtoListFlow);
	}
	
	
	@Override
	public Integer getAllActiveCommunitytoRerenceCount(CommunityCriteriaNew criteria, Integer first, Integer max, List<SortProperty> sortProperties, FormAccess formacc) {
	System.out.println(max+"----"+criteria.getErrorStatusEnum()+".........getAllActiveCommunitytoRerenceCount--- "+criteria.getArea().getUuid());
		frmsAccess = formacc;
		errorStatusEnum = criteria.getErrorStatusEnum();
		
		if(max > 47483647) {
			first = 1;
			max = 100;
		}
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Community> cq = cb.createQuery(Community.class);
			Root<Community> community = cq.from(Community.class);
			Join<Community, District> district = community.join(Community.DISTRICT, JoinType.LEFT);
			Join<District, Region> region = district.join(District.REGION, JoinType.LEFT);
	
					
			Predicate filter = null;
			if (criteria != null) {
				filter = service.buildCriteriaFilter(criteria, cb, community);
			} 

			if (filter != null) {
				cq.where(filter);
			}
			
			cq.orderBy(cb.asc(region.get(Region.NAME)), cb.asc(district.get(District.NAME)), cb.asc(community.get(Community.NAME)));
			
			cq.select(community);
			
			Integer finalResult = QueryHelper.getResultList(em, cq, first, max, this::toDtoList).stream().filter(e -> e.getFormAccess() != null).collect(Collectors.toList()).size();
			System.out.println("================ "+finalResult);
			
			//System.out.println("DEBUGGER r567ujhgty8ijyu8dfrf  " + SQLExtractor.from(em.createQuery(cq)));
			//if(isCounter)
			return finalResult;
			//.stream().filter(e -> e.getMessage() != "Correctly assigned").collect(Collectors.toList());
		}
	
	
	@Override
	public Integer getAllActiveCommunitytoRerenceFLowCount(CommunityCriteriaNew criteria, Integer first, Integer max, List<SortProperty> sortProperties, FormAccess formacc) {
	System.out.println(max+"----"+criteria.getErrorStatusEnum()+".........getAllActiveCommunitytoRerenceCount--- "+criteria.getArea().getUuid());
		frmsAccess = formacc;
	//	errorStatusEnum = criteria.getErrorStatusEnum();
		
		if(max > 47483647) {
			first = 1;
			max = 100;
		}
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Community> cq = cb.createQuery(Community.class);
			Root<Community> community = cq.from(Community.class);
			Join<Community, District> district = community.join(Community.DISTRICT, JoinType.LEFT);
			Join<District, Region> region = district.join(District.REGION, JoinType.LEFT);
	
					
			Predicate filter = null;
			if (criteria != null) {
				filter = service.buildCriteriaFilter(criteria, cb, community);
			} 

			if (filter != null) {
				cq.where(filter);
			}
			
			cq.orderBy(cb.asc(region.get(Region.NAME)), cb.asc(district.get(District.NAME)), cb.asc(community.get(Community.NAME)));
			
			cq.select(community);
			
			Integer finalResult = QueryHelper.getResultList(em, cq, first, max, this::toDtoListFlow).stream().filter(e -> e.getFormAccess() != null).collect(Collectors.toList()).size();
			System.out.println("================ "+finalResult);
			
			//System.out.println("DEBUGGER r567ujhgty8ijyu8dfrf  " + SQLExtractor.from(em.createQuery(cq)));
			//if(isCounter)
			return finalResult;
			//.stream().filter(e -> e.getMessage() != "Correctly assigned").collect(Collectors.toList());
		}
	

	@Override
	public List<CommunityDto> getIndexList(CommunityCriteriaNew criteria, Integer first, Integer max, List<SortProperty> sortProperties) {
		
		System.out.println("2222222222222222222222222444444444444444444442222222222222222222222222222 "+criteria.getArea());
		if(max > 47483647) {
			max = 100;
			}
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Community> cq = cb.createQuery(Community.class);
		Root<Community> community = cq.from(Community.class);
		Join<Community, District> district = community.join(Community.DISTRICT, JoinType.LEFT);
		Join<District, Region> region = district.join(District.REGION, JoinType.LEFT);

		Predicate filter = null;
		if (criteria != null) {
			filter = service.buildCriteriaFilter(criteria, cb, community);
		}

		if (filter != null) {
			cq.where(filter);
		}

		if (sortProperties != null && sortProperties.size() > 0) {
			List<Order> order = new ArrayList<Order>(sortProperties.size());
			for (SortProperty sortProperty : sortProperties) {
				Expression<?> expression;
				switch (sortProperty.propertyName) {
				case Community.NAME:
				case Community.GROWTH_RATE:
				case Community.EXTERNAL_ID:
				case Community.CLUSTER_NUMBER:
					expression = community.get(sortProperty.propertyName);
					break;
				case District.REGION:
				case CommunityDto.REGION_EXTERNALID:
				case CommunityDto.AREA_NAME:
				case CommunityDto.AREA_EXTERNAL_ID:
					expression = region.get(Region.NAME);
					break;
				case Community.DISTRICT:
				case CommunityDto.DISTRICT_EXTERNALID:
					expression = district.get(District.NAME);
					break;
				default:
					throw new IllegalArgumentException(sortProperty.propertyName);
				}
				order.add(sortProperty.ascending ? cb.asc(expression) : cb.desc(expression));
			}
			cq.orderBy(order);
		} else {
			cq.orderBy(cb.asc(region.get(Region.NAME)), cb.asc(district.get(District.NAME)), cb.asc(community.get(Community.NAME)));
		}

		cq.select(community);

		//		cq.multiselect(community.get(Community.CREATION_DATE), community.get(Community.CHANGE_DATE),
		//				community.get(Community.UUID), community.get(Community.NAME),
		//				region.get(Region.UUID), region.get(Region.NAME),
		//				district.get(District.UUID), district.get(District.NAME));

		return QueryHelper.getResultList(em, cq, first, max, this::toDto);
	}

	public Page<CommunityDto> getIndexPage(CommunityCriteriaNew communityCriteria, Integer offset, Integer size, List<SortProperty> sortProperties) {
		List<CommunityDto> communityList = getIndexList(communityCriteria, offset, size, sortProperties);
		long totalElementCount = count(communityCriteria);
		return new Page<>(communityList, offset, size, totalElementCount);
	}

	@Override
	public long count(CommunityCriteriaNew criteria) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<Community> root = cq.from(Community.class);

		Predicate filter = null;

		if (criteria != null) {
			filter = service.buildCriteriaFilter(criteria, cb, root);
		}

		if (filter != null) {
			cq.where(filter);
		}

		cq.select(cb.count(root));
		return em.createQuery(cq).getSingleResult();
	}
	
	

	@Override
	public List<String> getAllUuids() {

		if (userService.getCurrentUser() == null) {
			return Collections.emptyList();
		}

		return service.getAllUuids();
	}

	@Override
	public CommunityDto getByUuid(String uuid) {
		return toDto(service.getByUuid(uuid));
	}

	@Override
	public List<CommunityDto> getByUuids(List<String> uuids) {
		return service.getByUuids(uuids).stream().map(this::toDto).collect(Collectors.toList());
	}

	@Override
	public CommunityReferenceDto getCommunityReferenceByUuid(String uuid) {
		return toReferenceDto(service.getByUuid(uuid));
	}

	@Override
	public CommunityReferenceDto getCommunityReferenceById(long id) {
		return toReferenceDto(service.getById(id));
	}

	@Override
	public Map<String, String> getDistrictUuidsForCommunities(List<CommunityReferenceDto> communities) {

		if (communities.isEmpty()) {
			return new HashMap<>();
		}

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
		Root<Community> root = cq.from(Community.class);
		Join<Community, District> districtJoin = root.join(Community.DISTRICT, JoinType.LEFT);

		Predicate filter = root.get(Community.UUID).in(communities.stream().map(ReferenceDto::getUuid).collect(Collectors.toList()));
		cq.where(filter);
		cq.multiselect(root.get(Community.UUID), districtJoin.get(District.UUID));

		return em.createQuery(cq).getResultList().stream().collect(Collectors.toMap(e -> (String) e[0], e -> (String) e[1]));
	}

	@Override
	public CommunityDto save(@Valid CommunityDto dto) throws ValidationRuntimeException {
		return save(dto, false);
	}

	@Override
	public CommunityDto save(@Valid CommunityDto dto, boolean allowMerge) throws ValidationRuntimeException {
		checkInfraDataLocked();

		if (dto.getDistrict() == null) {
			throw new ValidationRuntimeException(I18nProperties.getValidationError(Validations.validDistrict));
		}

		Community community = service.getByUuid(dto.getUuid());

		if (community == null) {
			List<CommunityReferenceDto> duplicates = getByName(dto.getName(), dto.getDistrict(), true);
			if (!duplicates.isEmpty()) {
				if (allowMerge) {
					String uuid = duplicates.get(0).getUuid();
					community = service.getByUuid(uuid);
					CommunityDto dtoToMerge = getByUuid(uuid);
					dto = DtoHelper.copyDtoValues(dtoToMerge, dto, true);
				} else {
					throw new ValidationRuntimeException(I18nProperties.getValidationError(Validations.importCommunityAlreadyExists));
				}
			}
		}
		community = fillOrBuildEntity(dto, community, true);
		service.ensurePersisted(community);
		return toDto(community);
	}

	@Override
	public List<CommunityReferenceDto> getByName(String name, DistrictReferenceDto districtRef, boolean includeArchivedEntities) {

		return service.getByName(name, districtService.getByReferenceDto(districtRef), includeArchivedEntities)
			.stream()
			.map(CommunityFacadeEjb::toReferenceDto)
			.collect(Collectors.toList());
	}
	
	@Override
	public List<CommunityReferenceDto> getByExternalID(Long ext_id, DistrictReferenceDto districtRef, boolean includeArchivedEntities) {

		return service.getByExternalId(ext_id, districtService.getByReferenceDto(districtRef), includeArchivedEntities)
			.stream()
			.map(CommunityFacadeEjb::toReferenceDto)
			.collect(Collectors.toList());
	}

	@Override
	public List<CommunityReferenceDto> getByExternalId(Long externalId, boolean includeArchivedEntities) {

		return service.getByExternalId(externalId, includeArchivedEntities)
			.stream()
			.map(CommunityFacadeEjb::toReferenceDto)
			.collect(Collectors.toList());
	}

	@Override
	public List<CommunityReferenceDto> getReferencesByName(String name, boolean includeArchived) {
		return getByName(name, null, false);
	}

	@Override
	public boolean isUsedInOtherInfrastructureData(Collection<String> communityUuids) {
		return service.isUsedInInfrastructureData(communityUuids, Facility.COMMUNITY, Facility.class);
	}

	@Override
	public boolean hasArchivedParentInfrastructure(Collection<String> communityUuids) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<Community> root = cq.from(Community.class);
		Join<Community, District> districtJoin = root.join(Community.DISTRICT);
		Join<District, Region> regionJoin = districtJoin.join(District.REGION);

		cq.where(
			cb.and(
				cb.or(cb.isTrue(districtJoin.get(District.ARCHIVED)), cb.isTrue(regionJoin.get(Region.ARCHIVED))),
				root.get(Community.UUID).in(communityUuids)));

		cq.select(root.get(Community.ID));

		return QueryHelper.getFirstResult(em, cq) != null;
	}

	public static CommunityReferenceDto toReferenceDto(Community entity) {

		if (entity == null) {
			return null; 
		}
		
		CommunityReferenceDto dto = new CommunityReferenceDto(entity.getUuid(), entity.toString(), entity.getExternalId(), entity.getClusterNumber());
		return dto;
	}

	private CommunityDto toDto(Community entity) {

		if (entity == null) {
			return null;
		}
		CommunityDto dto = new CommunityDto();
		DtoHelper.fillDto(dto, entity);

		dto.setName(entity.getName());
		dto.setGrowthRate(entity.getGrowthRate());
		dto.setDistrict(DistrictFacadeEjb.toReferenceDto(entity.getDistrict()));
		dto.setDistrictexternalId(entity.getDistrict().getExternalId());
		dto.setRegion(RegionFacadeEjb.toReferenceDto(entity.getDistrict().getRegion()));
		dto.setRegionexternalId(entity.getDistrict().getRegion().getExternalId());
		dto.setArchived(entity.isArchived());
		dto.setExternalId(entity.getExternalId());
		dto.setClusterNumber(entity.getClusterNumber());
		dto.setAreaname(entity.getDistrict().getRegion().getArea().getName());
		dto.setAreaexternalId(entity.getDistrict().getRegion().getArea().getExternalId());

		return dto;
	}
	
	
	private CommunityUserReportModelDto toDtoList(Community entity) {
	
		if (entity == null) {
			return null;
		}
		
		
		//userService = new UserService();
		
		CommunityUserReportModelDto dto = new CommunityUserReportModelDto();
		DtoHelper.fillDto(dto, entity);

		dto.setRegion(entity.getDistrict().getRegion().getName());
		dto.setDistrict(entity.getDistrict().getName());
		dto.setCommunity(entity.getName());
		dto.setClusterNumber(entity.getClusterNumber().toString());

		dto.setcCode(entity.getExternalId().toString());
	
		dto.setArea(entity.getDistrict().getRegion().getArea().getName());
		
		List<String> usersd = new ArrayList<>();
		Set<FormAccess> formss = new HashSet<>();
		
		//execution time will be slow
		for(User usr : userService.getAllByCommunity()) {
				if(usr.getFormAccess().contains(frmsAccess)) {
				usr.getCommunity().stream().filter(ee -> ee.getUuid().equals(entity.getUuid())).findFirst().ifPresent(ef -> usersd.add(usr.getUserName()));
				usr.getCommunity().stream().filter(ee -> ee.getUuid().equals(entity.getUuid())).findFirst().ifPresent(ef -> formss.add(frmsAccess));
				}
			}
		dto.setFormAccess(formss);
		
		if(errorStatusEnum.equals(errorStatusEnum.ALL_REPORT)) {
		if(usersd.isEmpty()) {
			dto.setMessage("ClusterNumber: "+ entity.getClusterNumber()+" is not assigned to any user");
			dto.setUsername("no user");
		}else if(usersd.size() > 1){
			dto.setMessage("ClusterNumber: "+ entity.getClusterNumber()+" is assigned to more than one user");
			dto.setUsername(usersd.toString());
		} else {
			dto.setMessage("Correctly assigned");
			dto.setUsername(usersd.toString());
		}
		} else if(errorStatusEnum.equals(errorStatusEnum.ERROR_REPORT)) {
			
			if(usersd.isEmpty()) {
				dto.setMessage("ClusterNumber: "+ entity.getClusterNumber()+" is not assigned to any user");
				dto.setUsername("no user");
			}else if(usersd.size() > 1){
				dto.setMessage("ClusterNumber: "+ entity.getClusterNumber()+" is assigned to more than one user");
				dto.setUsername(usersd.toString());
			} else {
				dto = new CommunityUserReportModelDto(); 
			}	
		
		}
		
		
		
		return dto;
		
	}
	
	
	private CommunityUserReportModelDto toDtoListFlow(Community entity) {
		
		if (entity == null) {
			return null;
		}
		
		
		//userService = new UserService();
		
		CommunityUserReportModelDto dto = new CommunityUserReportModelDto();
		DtoHelper.fillDto(dto, entity);

		dto.setRegion(entity.getDistrict().getRegion().getName());
		dto.setDistrict(entity.getDistrict().getName());
		dto.setCommunity(entity.getName());
		dto.setClusterNumber(entity.getClusterNumber().toString());

		dto.setcCode(entity.getExternalId().toString());
	
		dto.setArea(entity.getDistrict().getRegion().getArea().getName());
		
		List<String> usersd = new ArrayList<>();
		Set<FormAccess> formss = new HashSet<>();
		
		//execution time will be slow
		for(User usr : userService.getAllByCommunity()) {
				if(usr.getFormAccess().contains(frmsAccess)) {
				usr.getCommunity().stream().filter(ee -> ee.getUuid().equals(entity.getUuid())).findFirst().ifPresent(ef -> usersd.add(usr.getUserName()));
				usr.getCommunity().stream().filter(ee -> ee.getUuid().equals(entity.getUuid())).findFirst().ifPresent(ef -> formss.add(frmsAccess));
				}
			}
		dto.setFormAccess(formss);
		
//		if(errorStatusEnum.equals(errorStatusEnum.ALL_REPORT)) {
//		if(usersd.isEmpty()) {
//			dto.setMessage("ClusterNumber: "+ entity.getClusterNumber()+" is not assigned to any user");
//			dto.setUsername("no user");
//		}else if(usersd.size() > 1){
//			dto.setMessage("ClusterNumber: "+ entity.getClusterNumber()+" is assigned to more than one user");
//			dto.setUsername(usersd.toString());
//		} else {
//			dto.setMessage("Correctly assigned");
//			dto.setUsername(usersd.toString());
//		}
//		} else if(errorStatusEnum.equals(errorStatusEnum.ERROR_REPORT)) {
//			
			if(usersd.isEmpty()) {
				dto.setMessage("ClusterNumber: "+ entity.getClusterNumber()+" is not assigned to any user");
				dto.setUsername("no user");
			}else if(usersd.size() > 1){
				dto.setMessage("ClusterNumber: "+ entity.getClusterNumber()+" is assigned to more than one user");
				dto.setUsername(usersd.toString());
			} else {
				dto = new CommunityUserReportModelDto(); 
			}	
		
	//	}
		
		
		
		return dto;
		
	}

	
	
	private Community fillOrBuildEntity(@NotNull CommunityDto source, Community target, boolean checkChangeDate) {

		target = DtoHelper.fillOrBuildEntity(source, target, Community::new, checkChangeDate);

		target.setName(source.getName());
		target.setGrowthRate(source.getGrowthRate());
		target.setDistrict(districtService.getByReferenceDto(source.getDistrict()));
		target.setArchived(source.isArchived());
		target.setExternalId(source.getExternalId());
		target.setClusterNumber(source.getClusterNumber());

		return target;
	}

	@LocalBean
	@Stateless
	public static class CommunityFacadeEjbLocal extends CommunityFacadeEjb {

		public CommunityFacadeEjbLocal() {
		}

		@Inject
		protected CommunityFacadeEjbLocal(CommunityService service, FeatureConfigurationFacadeEjbLocal featureConfiguration) {
			super(service, featureConfiguration);
		}
	}


	public static Set<CommunityReferenceDto> toReferenceDto(Set<Community> community) { //save
		
		Set<CommunityReferenceDto> dtos = new HashSet<CommunityReferenceDto>();
		for(Community com : community) {	
			CommunityReferenceDto dto = new CommunityReferenceDto(com.getUuid(), com.toString(), com.getExternalId(), com.getClusterNumber());	
			dtos.add(dto);
		}
		
		return dtos;
	}

	@Override
	public List<CommunityUserReportModelDto> getAllActiveCommunitytoRerencexx(CommunityCriteriaNew criteria,
			Integer first, Integer max, List<SortProperty> sortProperties, FormAccess formacc) {
		System.out.println("22222222222222222222222222222 1.0.3 222222222222222222222222222222222222222222222222222222222222 "+criteria.getArea().getUuid());
		frmsAccess = formacc;
		if(max > 47483647) {
			first = 1;
			max = 100;
			}
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Community> cq = cb.createQuery(Community.class);
			Root<Community> community = cq.from(Community.class);
			Join<Community, District> district = community.join(Community.DISTRICT, JoinType.LEFT);
			Join<District, Region> region = district.join(District.REGION, JoinType.LEFT);
	
					
			Predicate filter = null;
			if (criteria != null) {
				filter = service.buildCriteriaFilter(criteria, cb, community);
			} 

			if (filter != null) {
				cq.where(filter);
			}
			
			cq.orderBy(cb.asc(region.get(Region.NAME)), cb.asc(district.get(District.NAME)), cb.asc(community.get(Community.NAME)),  cb.asc(community.get(Community.EXTERNAL_ID)));
			
			cq.select(community);

			System.out.println("DEBUGGER r567ujhgty8asdfaveijyu8dfrf  " + SQLExtractor.from(em.createQuery(cq)));
			//if(isCounter)
			return QueryHelper.getResultList(em, cq, null, null, this::toDtoList);
		// TODO Auto-generated method stub
	
	}@Override
	public List<CommunityDto> getAllCommunities() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Community> cq = cb.createQuery(Community.class);
		Root<Community> comununity = cq.from(Community.class);
		Join<Community, District> district = comununity.join(Community.DISTRICT, JoinType.LEFT);
		Join<District, Region> region = district.join(District.REGION, JoinType.LEFT);
		Join<Region, Area> area = region.join(Region.AREA, JoinType.LEFT);
		
		Predicate filter = cb.equal(comununity.get(Community.ARCHIVED), false);
		cq.where(filter);
		cq.select(comununity); 
		List<Community> communities = em.createQuery(cq).getResultList();
		List<CommunityDto> dtos = new ArrayList();
		
		for (Community com : communities) {
			if (!com.equals(null)) {
				dtos.add(this.toDto(com));
			}
		}
		return dtos;
	}
	
	@Override
	public List<CommunityUserReportModelDto> getAllActiveCommunitytoRerenceNew(Integer first, Integer max, List<SortProperty> sortProperties, FormAccess formacc) {
	System.out.println("2222222222cccccccccccccccccccccccccccc22222222 ");
		frmsAccess = formacc;
		
		if(max > 47483647) {
			first = 1;
			max = 100;
			}
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Community> cq = cb.createQuery(Community.class);
			Root<Community> community = cq.from(Community.class);
			Join<Community, District> district = community.join(Community.DISTRICT, JoinType.LEFT);
			Join<District, Region> region = district.join(District.REGION, JoinType.LEFT);
	
					
			Predicate filter = null;
//			if (criteria != null) {
//				filter = service.buildCriteriaFilter(criteria, cb, community);
//			} 

			if (filter != null) {
				cq.where(filter);
			}
			
			cq.orderBy(cb.asc(region.get(Region.NAME)), cb.asc(district.get(District.NAME)), cb.asc(community.get(Community.NAME)));
			
			cq.select(community);

			System.out.println("DEBUGGER r56734rdujhgty8ijyu8dfrf  " + SQLExtractor.from(em.createQuery(cq)));
			//if(isCounter)
			return QueryHelper.getResultList(em, cq, null, null, this::toDtoList);//.stream().filter(e -> e.getMessage() != "Correctly assigned").collect(Collectors.toList());
		}

}
