package com.networknt.handler.util;

/**
 * A container class for providing implementations of utility interfaces for boilerplate tasks such as
 * content rendering, redirect, header, query and path param extraction.
 *
 * @author Bill O'Neil (https://github.com/billoneil)
 * @author Sachin Walia (https://github.com/sachinwalia2k8)
 *
 */
public class Exchange {

	public static interface ContentTypeSendersImpl extends ContentTypeSenders {
	}

	;
	private static final ContentTypeSendersImpl CONTENT_TYPE_SENDERS = new ContentTypeSendersImpl() {
	};

	public static ContentTypeSendersImpl contentTypeSenders() {
		return CONTENT_TYPE_SENDERS;
	}

	public static interface RedirectImpl extends RedirectSenders {
	}

	;
	private static final RedirectImpl REDIRECT = new RedirectImpl() {
	};

	public static RedirectImpl redirect() {
		return REDIRECT;
	}

	public static interface QueryParamImpl extends QueryParams {
	}

	;
	private static final QueryParamImpl QUERYPARAMS = new QueryParamImpl() {
	};

	public static QueryParamImpl queryParams() {
		return QUERYPARAMS;
	}

	public static interface PathParamImpl extends PathParams {
	}

	;
	private static final PathParamImpl PATHPARAMS = new PathParamImpl() {
	};

	public static PathParamImpl pathParams() {
		return PATHPARAMS;
	}

	public static interface HeaderImpl extends Headers {
	}

	;
	private static final HeaderImpl HEADERS = new HeaderImpl() {
	};

	public static HeaderImpl headers() {
		return HEADERS;
	}
}
