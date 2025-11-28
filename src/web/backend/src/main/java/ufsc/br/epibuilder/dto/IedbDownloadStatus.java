package ufsc.br.epibuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IedbDownloadStatus {

    private boolean inProgress;
    private String progressMessage;
    private boolean success;
    
}
