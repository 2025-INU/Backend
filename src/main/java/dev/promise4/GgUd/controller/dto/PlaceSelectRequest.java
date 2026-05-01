package dev.promise4.GgUd.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSelectRequest {

    @NotBlank
    private String placeId;

    private Long queryId;
}
