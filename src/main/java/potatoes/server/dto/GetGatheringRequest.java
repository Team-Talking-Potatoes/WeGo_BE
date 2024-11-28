package potatoes.server.dto;

import java.util.List;

import lombok.Builder;
import potatoes.server.constant.GatheringType;
import potatoes.server.utils.Pagination.Paginator;

@Builder
public record GetGatheringRequest(
	List<Long> ids,

	GatheringType type,

	String location,

	String date,

	Long createdBy,

	String sortBy,

	String sortOrder,

	Integer limit,
	Integer offset
) implements Paginator {

	@Override
	public int getOffset() {
		return offset != null ? offset : 20;
	}

	@Override
	public int getLimit() {
		return limit != null ? limit : 0;
	}

	@Override
	public String getSortBy() {
		return sortBy != null ? sortBy : "dateTime";
	}

	@Override
	public String getSortOrder() {
		return sortOrder != null ? sortOrder : "asc";
	}

}
