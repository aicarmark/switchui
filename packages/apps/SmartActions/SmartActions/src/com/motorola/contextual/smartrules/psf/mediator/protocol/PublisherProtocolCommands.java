package com.motorola.contextual.smartrules.psf.mediator.protocol;

public interface PublisherProtocolCommands {

	public static final String SUBSCRIBE_EVENT 			= "subscribe_request";
	public static final String CANCEL_EVENT 			= "cancel_request";
	public static final String REFRESH_EVENT 			= "refresh_request";
	public static final String LIST_EVENT 				= "list_request";
	public static final String SUBSCRIBE_RESPONSE_EVENT = "subscribe_response";
	public static final String CANCEL_RESPONSE_EVENT 	= "cancel_response";
	public static final String REFRESH_RESPONSE_EVENT 	= "refresh_response";
	public static final String LIST_RESPONSE_EVENT 		= "list_response";
	public static final String NOTIFY_EVENT 			= "notify";
	public static final String INITIATE_REFRESH 		= "initiate_refresh_request";

}