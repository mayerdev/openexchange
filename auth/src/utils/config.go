package utils

import (
	"github.com/spf13/viper"
)

type ConfigFile struct {
	Listen string `mapstructure:"listen"`

	Database struct {
		Host     string `mapstructure:"host"`
		Port     int    `mapstructure:"port"`
		User     string `mapstructure:"user"`
		Password string `mapstructure:"password"`
		Database string `mapstructure:"database"`
	} `mapstructure:"database"`

	Redis struct {
		Host string `mapstructure:"host"`
		Port int    `mapstructure:"port"`
	} `mapstructure:"redis"`

	NATS struct {
		Host string `mapstructure:"host"`
		Port int    `mapstructure:"port"`
	}
}

var Config ConfigFile

func LoadConfig() {
	viper.SetConfigFile(".env")
	viper.SetConfigType("env")

	if err := viper.ReadInConfig(); err != nil {
		panic(err)
	}

	viper.AddConfigPath(".")

	viper.SetDefault("listen", "0.0.0.0:3000")
	viper.SetDefault("database.host", "localhost")
	viper.SetDefault("database.port", 5432)
	viper.SetDefault("database.user", "postgres")
	viper.SetDefault("database.password", "password")
	viper.SetDefault("database.database", "openexchange")
	viper.SetDefault("redis.host", "localhost")
	viper.SetDefault("redis.port", 6379)

	viper.SetDefault("nats.host", "localhost")
	viper.SetDefault("nats.port", 4222)

	viper.SetEnvPrefix("OEX")
	viper.AutomaticEnv()

	if err := viper.Unmarshal(&Config); err != nil {
		panic(err)
	}
}
