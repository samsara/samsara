require 'simplecov'

if ENV['COVERAGE']
  SimpleCov.start do
    add_filter '/spec/'
  end
end

RSpec.configure do |config|
  config.expect_with :rspec do |c|
    c.syntax = :expect
  end
end

require 'samsara_sdk'

require 'webmock/rspec'
WebMock.disable_net_connect!(allow_localhost: true)
