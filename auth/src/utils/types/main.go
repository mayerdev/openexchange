package types

import (
	"github.com/gofiber/fiber/v3"
)

type Error struct {
	Reason  string `json:"reason"`
	Message string `json:"message"`
}

var NoErrors = make([]Error, 0)

type ErrorResponse struct {
	Message string  `json:"message"`
	Errors  []Error `json:"errors"`
}

func EmitError(ctx fiber.Ctx, code int, message string, errors []Error) error {
	res := ErrorResponse{
		Message: message,
		Errors:  errors,
	}

	return ctx.Status(code).JSON(res)
}
