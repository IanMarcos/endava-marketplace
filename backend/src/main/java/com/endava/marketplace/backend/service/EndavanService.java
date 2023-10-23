package com.endava.marketplace.backend.service;

import com.endava.marketplace.backend.model.Endavan;
import com.endava.marketplace.backend.repository.EndavanRepository;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.Optional;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Service
public class EndavanService {
    private static final String GRAPH_ME_ENDPOINT = "https://graph.microsoft.com/v1.0/me";
    private static final String GRAPH_PICTURE_ENDPOINT = "https://graph.microsoft.com/v1.0/me/photo/$value";
    private final EndavanRepository endavanRepository;

    public EndavanService(EndavanRepository endavanRepository) {
        this.endavanRepository = endavanRepository;
    }

    public Endavan saveEndavan() {
        Endavan endavan = getEndavanInfo();
        Optional<Endavan> savedEndavan = endavanRepository.findEndavanByEmailIgnoreCase(endavan.getEmail());
        if (savedEndavan.isEmpty()){
            return endavanRepository.save(endavan);
        }
        endavan = savedEndavan.get();
        return endavan;
    }

    public Optional<Endavan> findEndavanById(Integer endavanId) {return endavanRepository.findById(endavanId);}

    public void deleteEndavanById(Integer endavanId) {endavanRepository.deleteById(endavanId);}

    private Endavan getEndavanInfo(){
        Authentication authentication = getAuthentication();
        JwtAuthenticationToken auth = (JwtAuthenticationToken) authentication;
        Jwt principal = (Jwt) auth.getPrincipal();
        String name  = principal.getClaim("name");
        String email = principal.getClaim("preferred_username");
        if (email == null){
            email = principal.getClaim("upn");
        }
        System.out.println(email);

        return new Endavan(null, name, email, false, null, null, null);
    }

    protected Authentication getAuthentication(){
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public byte[] getGraphPicture(OAuth2AuthorizedClient graph, WebClient webClient) {
        if (null != graph) {
            byte[] body = webClient
                    .get()
                    .uri(GRAPH_PICTURE_ENDPOINT)
                    .attributes(oauth2AuthorizedClient(graph))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            return body;
        }
        else return null;
    }
}
