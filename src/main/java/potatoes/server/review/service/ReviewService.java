package potatoes.server.review.service;

import static potatoes.server.utils.error.ErrorCode.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import potatoes.server.infra.s3.S3UtilsProvider;
import potatoes.server.review.dto.CreateReviewRequest;
import potatoes.server.review.dto.GetDetailsReview;
import potatoes.server.review.dto.GetMyReviewResponse;
import potatoes.server.review.dto.GetReviewInTravel;
import potatoes.server.review.dto.GetReviewResponse;
import potatoes.server.review.dto.SimpleReviewResponse;
import potatoes.server.review.dto.TotalCountReviews;
import potatoes.server.review.dto.TotalRatingResponse;
import potatoes.server.review.entity.Review;
import potatoes.server.review.entity.ReviewImage;
import potatoes.server.review.entity.ReviewLike;
import potatoes.server.review.repository.ReviewLikeRepository;
import potatoes.server.review.repository.ReviewRepository;
import potatoes.server.travel.entity.Travel;
import potatoes.server.travel.repository.TravelRepository;
import potatoes.server.user.entity.User;
import potatoes.server.user.repository.UserRepository;
import potatoes.server.utils.constant.SortByType;
import potatoes.server.utils.error.exception.WeGoException;
import potatoes.server.utils.pagination.dto.PageResponse;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final UserRepository userRepository;
	private final TravelRepository travelRepository;
	private final ReviewLikeRepository reviewLikeRepository;
	private final S3UtilsProvider s3;

	@Transactional
	public void createReview(CreateReviewRequest request, Long userId) {

		User user = userRepository.findById(userId).orElseThrow(() -> new WeGoException(USER_NOT_FOUND));
		Travel travel = travelRepository.findById(request.travelId()).orElseThrow(
			() -> new WeGoException(TRAVEL_NOT_FOUND)
		);

		Review review = Review.builder()
			.travel(travel)
			.commenter(user)
			.title(request.title())
			.comment(request.comment())
			.starRating(request.starRating())
			.organizerReviewTags(request.organizerReviewTags())
			.reviewImages(new ArrayList<>())
			.build();

		List<String> fileNames = s3.uploadFiles(request.images());
		List<String> imageUrls = fileNames.stream()
			.map(s3::getFileUrl)
			.toList();

		List<ReviewImage> reviewImages = imageUrls.stream()
			.map(url -> ReviewImage.builder()
				.review(review)
				.imageUrl(url)
				.build())
			.toList();

		review.getReviewImages().addAll(reviewImages);
		reviewRepository.save(review);
	}

	public GetDetailsReview getDetailsReview(Long reviewId, Optional<Long> userId) {
		int reviewLikes = reviewLikeRepository.countAllByReviewId(reviewId);
		Review review = reviewRepository.findReviewWithImagesAndCommenter(reviewId);

		Boolean likesFlag = reviewLikeRepository.existsByUserIdAndReviewIdWithNull(userId, reviewId);

		return GetDetailsReview.from(review, reviewLikes, likesFlag);
	}

	public PageResponse<GetReviewResponse> getReviews(SortByType sortByType, int page, int size,
		Optional<Long> userId) {
		PageRequest pageable = PageRequest.of(page, size);
		Page<GetReviewResponse> findReviews = getReviewsWithSort(sortByType, pageable, userId);
		return PageResponse.from(findReviews);
	}

	private Page<GetReviewResponse> getReviewsWithSort(SortByType sortByType, Pageable pageable,
		Optional<Long> userId) {
		return switch (sortByType) {
			case LATEST -> userId
				.map(uid -> reviewRepository.findAllByOrderByCreatedAtDesc(pageable, uid))
				.orElseGet(() -> reviewRepository.findAllByOrderByCreatedAtDesc(pageable));
			case POPULAR -> userId
				.map(uid -> reviewRepository.findAllByOrderByLikesCountDesc(pageable, uid))
				.orElseGet(() -> reviewRepository.findAllByOrderByLikesCountDesc(pageable));
		};
	}

	@Transactional
	public void addReviewLike(Long reviewId, Long userId) {

		if (reviewLikeRepository.existsByUserIdAndReviewId(userId, reviewId)) {
			throw new WeGoException(REVIEW_LIKE_ALREADY_EXIST);
		}

		Review review = reviewRepository.findById(reviewId).orElseThrow(
			() -> new WeGoException(REVIEW_NOT_FOUND)
		);
		User user = userRepository.findById(userId).orElseThrow(() -> new WeGoException(USER_NOT_FOUND));

		ReviewLike reviewLike = ReviewLike.builder()
			.review(review)
			.user(user)
			.build();

		reviewLikeRepository.save(reviewLike);
	}

	@Transactional
	public void removeReviewLike(Long reviewId, Long userId) {
		ReviewLike reviewLike = reviewLikeRepository.findByUserIdAndReviewId(userId, reviewId).orElseThrow(
			() -> new WeGoException(REVIEW_LIKE_NOT_FOUND)
		);
		reviewLikeRepository.delete(reviewLike);
	}

	public List<SimpleReviewResponse> getSimpleReviews(int page, int size) {
		PageRequest request = PageRequest.of(page, size);
		return reviewRepository.findRecentReviews(request);
	}

	public PageResponse<GetMyReviewResponse> getMyReviews(int page, int size, Long userId) {
		PageRequest request = PageRequest.of(page, size);
		Page<GetMyReviewResponse> findReviews = reviewRepository.findMyReviews(request, userId);
		return PageResponse.from(findReviews);
	}

	public TotalRatingResponse getTotalReviewsRatings(Long travelId) {
		TotalCountReviews countedReviews = reviewRepository.countReviewsByRating(travelId);
		return countedReviews.toResponse();
	}

	public PageResponse<GetReviewInTravel> getReviewsInTravel(Long travelId, int page, int size) {
		PageRequest request = PageRequest.of(page, size);
		Page<GetReviewInTravel> findReviews = reviewRepository.findReviewByTravelId(request, travelId);
		return PageResponse.from(findReviews);
	}
}
