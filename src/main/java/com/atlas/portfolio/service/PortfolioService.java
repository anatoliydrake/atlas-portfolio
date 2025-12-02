package com.atlas.portfolio.service;

import com.atlas.portfolio.dto.request.CreatePortfolioRequest;
import com.atlas.portfolio.dto.request.UpdatePortfolioRequest;
import com.atlas.portfolio.dto.response.PortfolioResponse;
import com.atlas.portfolio.entity.Portfolio;
import com.atlas.portfolio.exception.ResourceNotFoundException;
import com.atlas.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    @Transactional
    public PortfolioResponse createPortfolio(CreatePortfolioRequest request, Long userId) {
        Portfolio portfolio = new Portfolio();
        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        portfolio.setUserId(userId);

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return new PortfolioResponse(savedPortfolio);
    }

    public List<PortfolioResponse> getAllPortfolios(Long userId) {
        return portfolioRepository.findByUserId(userId).stream()
                .map(PortfolioResponse::new)
                .collect(Collectors.toList());
    }

    public PortfolioResponse getPortfolioById(Long id, Long userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + id));
        return new PortfolioResponse(portfolio);
    }

    @Transactional
    public PortfolioResponse updatePortfolio(Long id, UpdatePortfolioRequest request, Long userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + id));

        if (request.getName() != null) {
            portfolio.setName(request.getName());
        }
        if (request.getDescription() != null) {
            portfolio.setDescription(request.getDescription());
        }

        Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
        return new PortfolioResponse(updatedPortfolio);
    }

    @Transactional
    public void deletePortfolio(Long id, Long userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + id));
        portfolioRepository.delete(portfolio);
    }
}
