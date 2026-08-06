package com.apiprecios.service;

import com.apiprecios.entity.Store;
import com.apiprecios.exception.BadRequestException;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    public List<Store> findAll() {
        return storeRepository.findAll();
    }

    public Store findById(Integer id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store", id));
    }

    public List<Store> search(String name) {
        return storeRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public Store create(Store store) {
        if (storeRepository.findByName(store.getName()).isPresent()) {
            throw new BadRequestException("Ya existe una tienda con el nombre '" + store.getName() + "'");
        }
        return storeRepository.save(store);
    }

    @Transactional
    public Store update(Integer id, Store data) {
        Store store = findById(id);

        boolean nameChanged = !store.getName().equals(data.getName());
        if (nameChanged && storeRepository.findByName(data.getName()).isPresent()) {
            throw new BadRequestException("Ya existe una tienda con el nombre '" + data.getName() + "'");
        }

        store.setName(data.getName());
        store.setBaseUrl(data.getBaseUrl());
        store.setLogoUrl(data.getLogoUrl());
        store.setAddress(data.getAddress());
        store.setLatitude(data.getLatitude());
        store.setLongitude(data.getLongitude());
        return storeRepository.save(store);
    }

    @Transactional
    public void delete(Integer id) {
        Store store = findById(id);
        storeRepository.delete(store);
    }
}
