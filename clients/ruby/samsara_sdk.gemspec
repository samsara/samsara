lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)

Gem::Specification.new do |spec|
  spec.authors = ['Egor Kolotaev']
  spec.summary = 'Ruby client library for Samsara Analytics platform.'
  spec.email = %w(samsara.systems+info@gmail.com)
  spec.homepage = 'https://github.com/samsara/samsara/tree/master/clients/ruby'
  spec.license = 'Apache License 2.0'
  spec.name = 'samsara_sdk'
  spec.require_paths = %w(lib)
  spec.required_ruby_version = '>= 1.9.3'
  spec.description = spec.summary
  spec.files = %w(LICENSE.txt README.md samsara_sdk.gemspec) + Dir['lib/**/*.rb']
  spec.version = File.read('../../samsara.version').strip.sub(/[A-za-z-]*$/, '')

  spec.add_development_dependency 'bundler'
  spec.add_development_dependency 'rspec'
  spec.add_development_dependency 'rubocop'
  spec.add_development_dependency 'simplecov'
  spec.add_development_dependency 'webmock'
end
