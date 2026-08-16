package com.zamp.invoice.model.extraction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.Rectangle;

/**
 * One word recognized by Tesseract, with its confidence and on-page position. {@code confidence}
 * is already normalized to 0–1 by the time this is constructed (Tess4J itself reports 0–100).
 * {@code boundingBox} lets {@code EvidenceMapper} disambiguate when the same text value appears
 * more than once on the page (e.g. a repeated line-item amount).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrWord {

    private String text;
    private float confidence;
    private Rectangle boundingBox;
}
