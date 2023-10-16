package edu.pnu.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.pnu.domain.Food;
import edu.pnu.domain.Note;
import edu.pnu.domain.SearchLog;
import edu.pnu.persistance.FoodRepository;
import edu.pnu.persistance.NoteRepository;
import edu.pnu.persistance.SearchLogRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoteService {

	@Autowired
	NoteRepository noteRepo;
	@Autowired
	FoodRepository foodRepo;
	@Autowired
	SearchLogRepository searchRepo;

	// 3.사용자별 조회
	public List<Note> getToday(String userId) {
		List<Note> notes = noteRepo.findByUserId(userId);
		for (Note note : notes) {
			if (note.getImageData() != null) {
				byte[] decodedImage = decodeImage(note.getImageData());
				note.setImageData(decodedImage);
			} else {
				note.setImageData(null);
			}
		}
		return notes;
	}

	// 4.식단 추가
	public Note insertNote(Note note, MultipartFile file) throws IOException {

		if (file != null && !file.isEmpty()) {
			byte[] imageBytes = file.getBytes(); // 이미지 바이트 배열 가져오기
			byte[] encodedImage = encodeImage(imageBytes);
			note.setImageData(encodedImage);
		}
		// 한국 시간에 맞추기 위해 9시간 더해줌
		LocalDateTime localDateTime = note.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		localDateTime = localDateTime.plusHours(9);
		Date newDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
		note.setDate(newDate);

		System.out.println(note.toString());

		return noteRepo.save(note);
	}

	// 5.음식 조회
	public List<Food> searchFood(String keyword) {
		try {
			int num = Integer.parseInt(keyword);
			List<Food> list = new ArrayList<>();
			list.add(foodRepo.findById(num).get());
			return list;
		} catch (Exception e) {
			return foodRepo.findByName(keyword);
		}
	}

	// 6.검색 기록 저장
	public List<Food> searchLog(String keyword) {
		System.out.println(keyword);
		List<Food> foods = foodRepo.findBySearchLog(keyword);
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		saveSearchLog(userId, keyword);

		return foods;
	}

	// 7.식단 수정
	public Note updateFood(Note note) {
		Optional<Note> optionalNote = noteRepo.findById(note.getId());

		if (!optionalNote.isPresent()) {
			throw new EntityNotFoundException("Id not present in the database");
		}

		Note n = optionalNote.get();
		n.setUserId(note.getUserId());
		n.setFoodName(note.getFoodName());
		n.setAmount(note.getAmount());
		n.setImageData(note.getImageData());
		n.setMealType(note.getMealType());

		return noteRepo.save(n);

	}

	// 8.식단 삭제
	public boolean deleteFood(Long id) {
		try {
			noteRepo.deleteById(id);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// 9. 최근 검색어 조회
	public List<SearchLog> searchKeyword(String userId) {

		return searchRepo.findByUserId(userId);
	}

	// 10. 최근 검색어 삭제
	@Transactional
	public boolean deleteKeyword(String keyword) {
		try {
			searchRepo.deleteByFoodName(keyword);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// ---------------------------------------------------------------------------------

	// 이미지 인코딩
	public byte[] encodeImage(byte[] imageBytes) {
		return Base64.getEncoder().encode(imageBytes);
	}

	// 이미지 디코딩
	public static byte[] decodeImage(byte[] imageData) {
		return Base64.getDecoder().decode(imageData);
	}

	// 7. 검색기록 저장
	private void saveSearchLog(String userId, String keyword) {

		SearchLog log = searchRepo.findByUserIdAndFoodName(userId, keyword);

		if (log != null) {
			searchRepo.deleteById(log.getId());
		}

		List<SearchLog> list = searchRepo.findAllByUserId(userId);
		if (list.size() == 15) {
			SearchLog firstLog = list.get(0);
			searchRepo.deleteById(firstLog.getId());
		}
		log = new SearchLog();
		log.setUserId(userId);
		log.setFoodName(keyword);
		searchRepo.save(log);
	}
}