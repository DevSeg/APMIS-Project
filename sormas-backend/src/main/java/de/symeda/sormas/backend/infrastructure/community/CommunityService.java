/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.backend.infrastructure.community;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.community.CommunityCriteriaNew;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.country.CountryReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.backend.common.AbstractInfrastructureAdoService;
import de.symeda.sormas.backend.common.CriteriaBuilderHelper;
import de.symeda.sormas.backend.infrastructure.area.Area;
import de.symeda.sormas.backend.infrastructure.country.Country;
import de.symeda.sormas.backend.infrastructure.country.CountryFacadeEjb.CountryFacadeEjbLocal;
import de.symeda.sormas.backend.infrastructure.district.District;
import de.symeda.sormas.backend.infrastructure.region.Region;

@Stateless
@LocalBean
public class CommunityService extends AbstractInfrastructureAdoService<Community> {

	@EJB
	private CountryFacadeEjbLocal countryFacade;

	public CommunityService() {
		super(Community.class);
	}

	public List<Community> getByName(String name, District district, boolean includeArchivedEntities) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Community> cq = cb.createQuery(getElementClass());
		Root<Community> from = cq.from(getElementClass());

		Predicate filter = CriteriaBuilderHelper.unaccentedIlikePrecise(cb, from.get(Community.NAME), name.trim());
		if (!includeArchivedEntities) {
			filter = cb.and(filter, createBasicFilter(cb, from));
		}
		if (district != null) {
			filter = cb.and(filter, cb.equal(from.get(Community.DISTRICT), district));
		}

		cq.where(filter);

		return em.createQuery(cq).getResultList();
	}

	public List<Community> getByExternalId(Long ext_id, District district_ext, boolean includeArchivedEntities) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Community> cq = cb.createQuery(getElementClass());
		Root<Community> from = cq.from(getElementClass());

		Predicate filter = cb.equal(from.get("externalId"), ext_id);

		if (!includeArchivedEntities) {
			filter = cb.and(filter, createBasicFilter(cb, from));
		}
		if (district_ext != null) {
			filter = cb.and(filter, cb.equal(from.get(Community.DISTRICT), district_ext));
		}

		cq.where(filter);

		return em.createQuery(cq).getResultList();
	}

	public List<Community> getByExternalId(Long externalId, boolean includeArchivedEntities) {
		return getByExternalId(externalId, Community.EXTERNAL_ID, includeArchivedEntities);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Predicate createUserFilter(CriteriaBuilder cb, CriteriaQuery cq, From<?, Community> from) {
		// no filter by user needed
		return null;
	}

	public Predicate buildCriteriaFilter(CommunityCriteriaNew criteria, CriteriaBuilder cb, Root<Community> from) {
		Join<Community, District> district = from.join(Community.DISTRICT, JoinType.LEFT);
		Join<District, Region> region = district.join(District.REGION, JoinType.LEFT);
		Join<Region, Area> area = region.join(Region.AREA, JoinType.LEFT);
		Predicate filter = null;

		CountryReferenceDto country = criteria.getCountry();
		if (country != null) {
			CountryReferenceDto serverCountry = countryFacade.getServerCountry();

			Path<Object> countryUuid = from.join(Community.DISTRICT, JoinType.LEFT).join(District.REGION, JoinType.LEFT)
					.join(Region.COUNTRY, JoinType.LEFT).get(Country.UUID);
			Predicate countryFilter = cb.equal(countryUuid, country.getUuid());

			if (country.equals(serverCountry)) {
				filter = CriteriaBuilderHelper.and(cb, filter,
						CriteriaBuilderHelper.or(cb, countryFilter, countryUuid.isNull()));
			} else {
				filter = CriteriaBuilderHelper.and(cb, filter, countryFilter);
			}
		}

		System.out.println("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz ");

		AreaReferenceDto aread = criteria.getArea();

		if (aread != null) {
			System.out.println("zzzzzzzzzzzzzzzzzzzz");
			filter = CriteriaBuilderHelper.and(cb, filter, cb.equal(area.get(Area.UUID), aread.getUuid()));
		}

		if (criteria.getRegion() != null) {
			filter = CriteriaBuilderHelper.and(cb, filter,
					cb.equal(region.get(Region.UUID), criteria.getRegion().getUuid()));
		}
		if (criteria.getDistrict() != null) {
			filter = CriteriaBuilderHelper.and(cb, filter,
					cb.equal(district.get(District.UUID), criteria.getDistrict().getUuid()));
		}
		if (criteria.getNameLike() != null) {
			String[] textFilters = criteria.getNameLike().split("\\s+");
			for (String textFilter : textFilters) {
				if (DataHelper.isNullOrEmpty(textFilter)) {
					continue;
				}

				Predicate likeFilters = CriteriaBuilderHelper.unaccentedIlike(cb, from.get(District.NAME), textFilter);
				filter = CriteriaBuilderHelper.and(cb, filter, likeFilters);
			}
		}
		if (criteria.getRelevanceStatus() != null) {
			if (criteria.getRelevanceStatus() == EntityRelevanceStatus.ACTIVE) {
				filter = CriteriaBuilderHelper.and(cb, filter,
						cb.or(cb.equal(from.get(Community.ARCHIVED), false), cb.isNull(from.get(Community.ARCHIVED))));
			} else if (criteria.getRelevanceStatus() == EntityRelevanceStatus.ARCHIVED) {
				filter = CriteriaBuilderHelper.and(cb, filter, cb.equal(from.get(Community.ARCHIVED), true));
			}
		}
		
		if(this.getCurrentUser().getArea() != null) {
			filter = CriteriaBuilderHelper.and(cb, filter,
					cb.equal(area.get(Area.UUID), this.getCurrentUser().getArea().getUuid()));
		}
		return filter;
	}

	public List<CommunityReferenceDto> getByDistrict(DistrictReferenceDto district) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<CommunityReferenceDto> cq = cb.createQuery(CommunityReferenceDto.class);
		Root<Community> root = cq.from(Community.class);
		Join<Community, District> communityDistrictJoin = root.join(Community.DISTRICT);

		Predicate filter = cb.equal(communityDistrictJoin.get(District.UUID), district.getUuid());
		cq.where(filter);
		cq.multiselect(root.get(Community.UUID), root.get(Community.NAME));

		TypedQuery query = em.createQuery(cq);
		return query.getResultList();
	}

	public Set<Community> getByReferenceDto(Set<CommunityReferenceDto> community) {
		Set<Community> communities = new HashSet<Community>(); 
		for (CommunityReferenceDto com : community) {
			if (com != null && com.getUuid() != null) {
				Community result = getByUuid(com.getUuid());
				if (result == null) {
					logger.warn("Could not find entity for " + com.getClass().getSimpleName() + " with uuid "
							+ com.getUuid());
				}
				communities.add(result);
			} 
		}
		
		System.out.println("22222222222222222222??????????????????" + communities);
		return communities;
	}

}
