package types

import (
	"net/http"

	"github.com/gofiber/fiber/v3"
)

type Error struct {
	Reason  string `json:"reason"`
	Message string `json:"message"`
}

var NoErrors = make([]Error, 0)

type ErrorCode int

const (
	ErrorNotFound           ErrorCode = 0
	ErrorValidation         ErrorCode = 1
	ErrorInvalidCredentials ErrorCode = 2
	ErrorInvalidRequest     ErrorCode = 3
	ErrorUserExists         ErrorCode = 4
	ErrorInvalidCode        ErrorCode = 5
	ErrorAttemptsExceeded   ErrorCode = 6
	ErrorTokenExpired       ErrorCode = 7
	ErrorTemporaryBlocked   ErrorCode = 8
	ErrorNoSessionID        ErrorCode = 9
	ErrorInvalidSession     ErrorCode = 10
	ErrorServerError        ErrorCode = 5000
)

type ErrorResponse struct {
	Code    ErrorCode `json:"code"`
	Message string    `json:"message"`
	Errors  []Error   `json:"errors"`
}

func (code ErrorCode) ToHTTP() int {
	switch code {
	case ErrorNotFound:
		return http.StatusNotFound
	case ErrorValidation:
		return http.StatusBadRequest
	case ErrorInvalidRequest:
		return http.StatusBadRequest
	case ErrorInvalidCredentials:
		return http.StatusBadRequest
	case ErrorUserExists:
		return http.StatusBadRequest
	case ErrorInvalidCode:
		return http.StatusBadRequest
	case ErrorAttemptsExceeded:
		return http.StatusBadRequest
	case ErrorTokenExpired:
		return http.StatusBadRequest
	case ErrorTemporaryBlocked:
		return http.StatusBadRequest
	case ErrorNoSessionID:
		return http.StatusBadRequest
	case ErrorInvalidSession:
		return http.StatusBadRequest
	case ErrorServerError:
		return http.StatusInternalServerError
	default:
		return http.StatusInternalServerError
	}
}

func EmitError(ctx fiber.Ctx, code ErrorCode, message string, errors []Error) error {
	httpCode := code.ToHTTP()
	res := ErrorResponse{
		Code:    code,
		Message: message,
		Errors:  errors,
	}

	return ctx.Status(httpCode).JSON(fiber.Map{"code": httpCode, "error": res})
}
