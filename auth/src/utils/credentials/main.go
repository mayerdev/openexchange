package credentials

import (
	"openexchange/auth/models"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

type CredentialType = string

const (
	CredentialTypeEmail CredentialType = "email"
	CredentialTypePhone CredentialType = "phone"
)

var SupportedCredentialTypes = []CredentialType{
	CredentialTypeEmail,
	CredentialTypePhone,
}

type CredentialPolicy struct {
	PasswordRequired bool
	BypassTFA        bool
}

var CredentialPolicies = map[CredentialType]CredentialPolicy{
	CredentialTypeEmail: {PasswordRequired: true, BypassTFA: false},
	CredentialTypePhone: {PasswordRequired: true, BypassTFA: false},
}

func FindTraderByCredential(db *gorm.DB, credType, value string) (*models.Trader, []models.TraderCredential, error) {
	var cred models.TraderCredential
	if err := db.Where("type = ? AND value = ?", credType, value).First(&cred).Error; err != nil {
		return nil, nil, err
	}

	var trader models.Trader
	if err := db.First(&trader, "id = ?", cred.TraderID).Error; err != nil {
		return nil, nil, err
	}

	var allCreds []models.TraderCredential
	if err := db.Where("trader_id = ?", cred.TraderID).Find(&allCreds).Error; err != nil {
		return nil, nil, err
	}

	return &trader, allCreds, nil
}

func UpsertCredential(db *gorm.DB, traderID uuid.UUID, credType, value string) (*models.TraderCredential, error) {
	var cred models.TraderCredential

	result := db.Where(models.TraderCredential{TraderID: traderID, Type: credType}).
		Assign(models.TraderCredential{Value: value}).
		FirstOrCreate(&cred)

	if result.Error != nil {
		return nil, result.Error
	}

	return &cred, nil
}
