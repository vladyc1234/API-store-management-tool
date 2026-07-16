package com.gamestore.game_store_api.common;

/**
 * Closed set of domain error categories used by feature-specific exceptions.
 */
public sealed abstract class StoreDomainException extends RuntimeException
		permits StoreDomainException.BadRequest, StoreDomainException.NotFound,
		StoreDomainException.Conflict, StoreDomainException.Forbidden {

	protected StoreDomainException(String message) {
		super(message);
	}

	public static non-sealed class BadRequest extends StoreDomainException {
		public BadRequest(String message) {
			super(message);
		}
	}

	public static non-sealed class NotFound extends StoreDomainException {
		public NotFound(String message) {
			super(message);
		}
	}

	public static non-sealed class Conflict extends StoreDomainException {
		public Conflict(String message) {
			super(message);
		}
	}

	public static non-sealed class Forbidden extends StoreDomainException {
		public Forbidden(String message) {
			super(message);
		}
	}
}
