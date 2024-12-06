package potatoes.server.service;

import static java.time.Duration.*;
import static potatoes.server.error.ErrorCode.*;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import potatoes.server.constant.ParticipantRole;
import potatoes.server.dto.CreateTravelRequest;
import potatoes.server.entity.Travel;
import potatoes.server.entity.TravelPlan;
import potatoes.server.entity.TravelUser;
import potatoes.server.entity.User;
import potatoes.server.error.exception.UserNotFound;
import potatoes.server.error.exception.WrongValueInCreateTravel;
import potatoes.server.repository.TravelPlanRepository;
import potatoes.server.repository.TravelRepository;
import potatoes.server.repository.TravelUserRepository;
import potatoes.server.repository.UserRepository;
import potatoes.server.utils.s3.S3UtilsProvider;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class TravelService {

	private final UserRepository userRepository;
	private final TravelRepository travelRepository;
	private final TravelPlanRepository travelPlanRepository;
	private final TravelUserRepository travelUserRepository;
	private final S3UtilsProvider s3;

	@Transactional
	public void createTravel(Long userId, CreateTravelRequest request) {
		if (request.minTravelMateCount() > request.maxTravelMateCount()) {
			throw new WrongValueInCreateTravel(INVALID_TRAVEL_MATE_COUNT);
		}

		if (request.hashTags().split("#").length > 5) {
			throw new WrongValueInCreateTravel(INVALID_TRAVEL_HASHTAGS_VALUE);
		}

		if (between(request.startAt(), request.endAt()).toDays() != (long)(request.tripDuration() - 1) ||
			request.startAt().isAfter(request.endAt())) {
			throw new WrongValueInCreateTravel(INVALID_TRAVEL_DATE);
		}

		for (CreateTravelRequest.DetailTravelRequest detailTravelRequest : request.detailTravel()) {
			if (detailTravelRequest.tripDay() > request.tripDuration()) {
				throw new WrongValueInCreateTravel(INVALID_TRAVEL_DETAIL_INFO);
			}
		}

		User user = userRepository.findById(userId).orElseThrow(UserNotFound::new);

		String travelImageUrl = s3.uploadFile(request.travelImage());
		Travel travel = Travel.builder()
			.name(request.travelName())
			.description(request.travelDescription())
			.image(travelImageUrl)
			.expectedTripCost(request.expectedTripCost())
			.minTravelMateCount(request.minTravelMateCount())
			.maxTravelMateCount(request.maxTravelMateCount())
			.hashTags(request.hashTags())
			.isDomestic(request.isDomestic())
			.travelLocation(request.travelLocation())
			.departureLocation(request.departureLocation())
			.startAt(request.startAt())
			.endAt(request.endAt())
			.tripDuration(request.tripDuration())
			.build();
		travelRepository.save(travel);

		List<TravelPlan> travelPlanList = request.detailTravel().stream()
			.map(details -> {
				String destinationImageUrl = s3.uploadFile(details.destinationImage());
				return TravelPlan.builder()
					.travel(travel)
					.image(destinationImageUrl)
					.tripDay(details.tripDay())
					.tripOrderNumber(details.tripOrderNumber())
					.destination(details.destination())
					.description(details.description())
					.build();
			}).toList();
		travelPlanRepository.saveAll(travelPlanList);

		TravelUser travelUser = TravelUser.builder()
			.role(ParticipantRole.ORGANIZER)
			.travel(travel)
			.user(user)
			.build();
		travelUserRepository.save(travelUser);
	}
}
