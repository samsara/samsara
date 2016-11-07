lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)

Gem::Specification.new do |spec|
  spec.authors = ['Egor Kolotaev']
  spec.description = 'Ruby client library for Samsara Analytics platform.'
  spec.email = %w(samsara.systems+info@gmail.com)
  spec.homepage = 'https://github.com/samsara/samsara'
  spec.licenses = ['Apache License 2.0']
  spec.name = 'samsara_sdk'
  spec.require_paths = %w(lib)
  spec.required_ruby_version = '>= 1.9.3'
  spec.summary = spec.description
  spec.version = '1.0.0'
end
