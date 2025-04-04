package com.example.mayoweb.store.repository;

import com.example.mayoweb.commons.annotation.FirestoreTransactional;
import com.example.mayoweb.commons.exception.ApplicationException;
import com.example.mayoweb.commons.exception.payload.ErrorStatus;
import com.example.mayoweb.store.domain.StoreEntity;
import com.example.mayoweb.store.domain.dto.request.UpdateStoreRequest;
import com.example.mayoweb.store.domain.dto.response.UpdateStoreResponse;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
@FirestoreTransactional
public class StoreAdapter {

    private static final String COLLECTION_NAME = "stores";
    private static final String TIME_PATTERN = "HH:mm";
    private final Firestore firestore;

    public DocumentReference getDocsRef(String storeId) {
        return firestore.collection(COLLECTION_NAME).document(storeId);
    }

    public Optional<StoreEntity> findByStoreId(String storeId) {

        DocumentReference documentReference = firestore.collection(COLLECTION_NAME).document(storeId);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = null;

        try {
            document = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ApplicationException(ErrorStatus.toErrorStatus("해당 가게를 찾는데 오류가 발생하였습니다", 400, LocalDateTime.now()));
        }

        return Optional.ofNullable(fromDocument(document));
    }

    public void closeStore(String storeId) {

        DocumentReference documentReference = firestore.collection(COLLECTION_NAME).document(storeId);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = null;

        try {
            document = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ApplicationException(ErrorStatus.toErrorStatus("가게를 닫는 중 오류가 발생하였습니다.", 400, LocalDateTime.now()));
        }

        if (document.exists()) {
            documentReference.update("open_state", false);
        }
    }

    public void openStore(String storeId) {

        DocumentReference documentReference = firestore.collection(COLLECTION_NAME).document(storeId);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = null;
        try {
            document = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ApplicationException(ErrorStatus.toErrorStatus("가게를 오픈하는 도중 오류가 발생하였습니다.", 400, LocalDateTime.now()));
        }
        if (document.exists()) {
            documentReference.update("open_state", true);
        }
    }

    public UpdateStoreResponse updateStore(UpdateStoreRequest storeRequest){

        DocumentReference documentReference = firestore.collection(COLLECTION_NAME).document(storeRequest.storeId());
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = null;

        try {
            document = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ApplicationException(ErrorStatus.toErrorStatus("스토어를 업데이트 하는 도중 알 수 없는 에러가 발생하였습니다.", 400, LocalDateTime.now()));
        }

        if (document.exists()) {
            documentReference.update(
                    "address", storeRequest.address(),
                    "store_name", storeRequest.storeName(),
                    "store_number", storeRequest.storeNumber(),
                    "open_time", storeRequest.openTime(),
                    "close_time", storeRequest.closeTime(),
                    "sale_start", storeRequest.saleStart(),
                    "sale_end", storeRequest.saleEnd(),
                    "additional_comment", storeRequest.additionalComment());

            return UpdateStoreResponse.builder()
                    .storeId(storeRequest.storeId())
                    .build();
        }

        throw new ApplicationException(ErrorStatus.toErrorStatus("스토어가 존재하지 않습니다.", 404, LocalDateTime.now()));
    }

    public List<StoreEntity> getCloseTimeStores(LocalDateTime now) {

        List<StoreEntity> storeList = new ArrayList<>();

        Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("open_state", true)
                .whereEqualTo("close_time", formattedTime(now));

        try {
            for (QueryDocumentSnapshot documentSnapshot : query.get().get()) {
                storeList.add(fromDocument(documentSnapshot));
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new ApplicationException(ErrorStatus.toErrorStatus(
                    "통신 중 오류가 발생하였습니다.", 500, LocalDateTime.now()
            ));
        }

        return storeList;
    }

    public List<StoreEntity> getOpenTimeStores(LocalDateTime currentTime) {

        List<StoreEntity> storeList = new ArrayList<>();
        Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("open_state", false)
                .whereEqualTo("sale_start", formattedTime(currentTime))
                .whereEqualTo("is_auto", true)
                .whereArrayContains("open_day_of_week", currentTime.getDayOfWeek().getValue());

        try {
            for (QueryDocumentSnapshot documentSnapshot : query.get().get()) {
                storeList.add(fromDocument(documentSnapshot));
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new ApplicationException(ErrorStatus.toErrorStatus(
                    "통신 중 오류가 발생하였습니다.", 500, LocalDateTime.now()
            ));
        }
        return storeList;
    }

    private String formattedTime(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_PATTERN);
        return time.format(formatter);
    }

    private StoreEntity fromDocument(DocumentSnapshot document) {
        return StoreEntity.builder()
                .id(document.getId())
                .storeName(document.getString("store_name"))
                .openState(document.getBoolean("open_state"))
                .address(document.getString("address"))
                .storeImage(document.getString("store_image"))
                .openTime(document.getString("open_time"))
                .closeTime(document.getString("close_time"))
                .saleStart(document.getString("sale_start"))
                .saleEnd(document.getString("sale_end"))
                .storeDescription(document.getString("store_description"))
                .storeNumber(document.getString("store_number"))
                .storeMapUrl(document.getString("store_map_url"))
                .originInfo(document.getString("origin_info"))
                .additionalComment(document.getString("additional_comment"))
                .build();
    }
}
