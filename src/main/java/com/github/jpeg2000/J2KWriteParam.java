package com.github.jpeg2000;

import jj2000.j2k.IntegerSpec;
import jj2000.j2k.ModuleSpec;
import jj2000.j2k.StringSpec;
import jj2000.j2k.entropy.CBlkSizeSpec;
import jj2000.j2k.entropy.PrecinctSizeSpec;
import jj2000.j2k.entropy.ProgressionSpec;
import jj2000.j2k.entropy.encoder.LayersInfo;
import jj2000.j2k.image.forwcomptransf.ForwCompTransfSpec;
import jj2000.j2k.quantization.GuardBitsSpec;
import jj2000.j2k.quantization.QuantStepSizeSpec;
import jj2000.j2k.quantization.QuantTypeSpec;
import jj2000.j2k.roi.MaxShiftSpec;
import jj2000.j2k.wavelet.analysis.AnWTFilterSpec;

/**
 * Interface which defines the parameters required to write a JP2 image.
 * Abstracted away from the horror that is J2KImageWriteParamJava
 */
public interface J2KWriteParam {

    public boolean getLossless();

    public int getNumComponents();

    public String getLayers();

    public IntegerSpec getDecompositionLevel();

    public PrecinctSizeSpec getPrecinctPartition();

    public ProgressionSpec getProgressionType();

    public void setProgressionType(LayersInfo lyrs, String values);

    public String getProgressionName();

    public StringSpec getSOP();

    public StringSpec getEPH();

    public ForwCompTransfSpec getComponentTransformation();

    public CBlkSizeSpec getCodeBlockSize();

    public StringSpec getBypass();

    public StringSpec getResetMQ();

    public StringSpec getTerminateOnByte();

    public StringSpec getCausalCXInfo();

    public StringSpec getMethodForMQLengthCalc();

    public StringSpec getMethodForMQTermination();

    public StringSpec getCodeSegSymbol();

    public AnWTFilterSpec getFilters();

    public QuantStepSizeSpec getQuantizationStep();

    public QuantTypeSpec getQuantizationType();

    public GuardBitsSpec getGuardBits();

    public MaxShiftSpec getROIs();

    public int getStartLevelROI();

    public boolean getAlignROI();

    public int getNumTiles();

}
