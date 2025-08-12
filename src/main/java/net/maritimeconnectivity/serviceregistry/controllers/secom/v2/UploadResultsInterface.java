package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import org.grad.secom.core.interfaces.GenericSecomInterface;
import org.grad.secomv2.core.base.SecomConstants;

public interface UploadResultsInterface extends GenericSecomInterface {

    String UPLOAD_RESULTS_INTERFACE_PATH = "/" + SecomConstants.SECOM_VERSION + "/uploadResults";



}
