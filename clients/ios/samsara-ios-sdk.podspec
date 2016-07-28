#
# Be sure to run `pod lib lint samsara-ios-sdk.podspec' to ensure this is a
# valid spec and remove all comments before submitting the spec.
#
# Any lines starting with a # are optional, but encouraged
#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#

Pod::Spec.new do |s|
  s.name             = "samsara-ios-sdk"
  s.version          = "0.5.5.0"
  s.summary          = "Samsara client library for iOS devices"
  s.description      = <<-DESC
                        samsara-ios-sdk is a client library for the Samsara Analytics platform. For more information visit the github page.
                       DESC
  s.homepage         = "https://github.com/samsara/samsara-ios-sdk"
  s.license          = 'Apache'
  s.author           = { "Sathyavijayan Vittal" => "sathyavijayan@gmail.com" }
  s.source           = { :git => "https://github.com/samsara/samsara.git", :tag => s.version.to_s }

  s.platform     = :ios, '8.0'
  s.requires_arc = true

  s.source_files = 'Pod/Classes/*'

  s.dependency 'Locksmith', '2.0.8'
  s.dependency 'OHHTTPStubs', '4.0.2'
  s.dependency 'OCMock', '3.1.2'
  s.dependency 'Reachability', '3.2'
end
