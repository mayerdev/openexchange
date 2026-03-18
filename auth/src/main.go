package main

import (
	"errors"
	"openexchange/auth/router"
	"openexchange/auth/utils"
	"openexchange/auth/utils/types"
	"reflect"
	"strings"

	"github.com/go-playground/validator/v10"
	"github.com/gofiber/fiber/v3"
	"github.com/gofiber/fiber/v3/middleware/cors"
)

func main() {
	utils.LoadConfig()

	v := validator.New()

	v.RegisterTagNameFunc(func(fld reflect.StructField) string {
		name := strings.SplitN(fld.Tag.Get("json"), ",", 2)[0]
		if name == "-" {
			return ""
		}

		return name
	})

	app := fiber.New(fiber.Config{
		StructValidator: &utils.StructValidator{Validator: v},
		ErrorHandler: func(ctx fiber.Ctx, err error) error {
			var ve validator.ValidationErrors
			if errors.As(err, &ve) {
				fieldErrors := make([]types.Error, len(ve))
				for i, fe := range ve {
					fieldErrors[i] = types.Error{
						Reason:  fe.Field(),
						Message: fe.Tag(),
					}
				}

				return types.EmitError(ctx, types.ErrorValidation, "Validation failed", fieldErrors)
			}

			var e *fiber.Error
			if errors.As(err, &e) {
				switch e.Code {
				case fiber.StatusNotFound:
					return types.EmitError(ctx, types.ErrorNotFound, e.Message, types.NoErrors)
				default:
					return types.EmitError(ctx, types.ErrorValidation, e.Message, types.NoErrors)
				}
			}

			return types.EmitError(ctx, types.ErrorServerError, "Internal server error", types.NoErrors)
		},
	})

	app.Use(cors.New(
		cors.Config{
			AllowOrigins: []string{"*"},
			AllowHeaders: []string{
				"Content-Type", "Authorization", "X-Requested-With", "X-CSRF-Token",
				"Accept", "Accept-Language", "Accept-Encoding", "X-Flow-UUID",
				"range", "Content-Range",
			},
			ExposeHeaders: []string{"Content-Length", "Content-Range", "Content-Type", "Date"},
			AllowMethods:  []string{"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"},
		},
	))

	router.Setup(app)

	if err := app.Listen(utils.Config.Listen); err != nil {
		panic(err)
	}
}
