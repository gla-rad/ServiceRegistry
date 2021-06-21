package net.maritimeconnectivity.serviceregistry.exceptions;

public class DataNotFoundException extends Exception {

    private static final long serialVersionUID = -2171229941490315105L;

    /**
     * Instantiates a new Data Not Found exception.
     *
     * @param message the message
     * @param t       the t
     */
    public DataNotFoundException(String message, Throwable t) {
        super(message, t);
    }

}
