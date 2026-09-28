package com.messmate.service;

import com.messmate.dto.ResidentDtos.ResidentRequest;
import com.messmate.dto.ResidentDtos.ResidentResponse;
import com.messmate.entity.Resident;
import com.messmate.exception.ApiException;
import com.messmate.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResidentService {

    private final ResidentRepository residents;

    public List<ResidentResponse> findAll() {
        return residents.findAll(Sort.by("roomNo", "name")).stream().map(ResidentResponse::from).toList();
    }

    public ResidentResponse get(Long id) {
        return ResidentResponse.from(find(id));
    }

    @Transactional
    public ResidentResponse create(ResidentRequest req) {
        if (residents.existsByEmailIgnoreCase(req.email().trim())) {
            throw ApiException.conflict("A resident with email " + req.email().trim() + " already exists.");
        }
        Resident r = new Resident();
        apply(r, req);
        return ResidentResponse.from(residents.save(r));
    }

    @Transactional
    public ResidentResponse update(Long id, ResidentRequest req) {
        Resident r = find(id);
        if (residents.existsByEmailIgnoreCaseAndIdNot(req.email().trim(), id)) {
            throw ApiException.conflict("Another resident already uses email " + req.email().trim() + ".");
        }
        apply(r, req);
        return ResidentResponse.from(r);
    }

    /** Also removes the resident's feedback (cascade) and opt-out rows (owning side of the join table). */
    @Transactional
    public void delete(Long id) {
        residents.delete(find(id));
    }

    Resident find(Long id) {
        return residents.findById(id).orElseThrow(() -> ApiException.notFound("Resident", id));
    }

    private void apply(Resident r, ResidentRequest req) {
        r.setName(req.name().trim());
        r.setRoomNo(req.roomNo().trim());
        r.setEmail(req.email().trim());
    }
}
