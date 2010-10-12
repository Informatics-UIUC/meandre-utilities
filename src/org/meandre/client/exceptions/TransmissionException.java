package org.meandre.client.exceptions;

/**
 * An exception that is thrown when there is a problem communicating with
 * a Meandre Infrastructure server through it's webservices. This is the
 * default exception the Meandre*Client classes use when they have a problem
 * accessing the server they're talking to.
 *
 * The typical scenarios this is used for are:
 * 1) server doesn't respond
 * 2) client doesn't undertand the response it gets back from a server
 *
 * @author Peter Groves
 */
public class TransmissionException extends Exception {

    private static final long serialVersionUID = 6312322678217327472L;

    /** default constructor for an exception that contains no information.
     */
    public TransmissionException() {
        super();
    }

    /** contains a message and wraps another type of exception (eg an 
     * exception thrown by a parser of the response from the server).
     */
    public TransmissionException(String message, Throwable sourceException) {
        super(message, sourceException);
    }

    /** contains a message about the problem. */
    public TransmissionException(String message) {
        super(message);
        
    }

    /** contains another exception that caused the original problem 
     * (timeout exception, exception from parsing the server's response, etc).
     */
    public TransmissionException(Throwable sourceException) {
        super(sourceException);
    }
}
