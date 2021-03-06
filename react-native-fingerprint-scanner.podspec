require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = package["name"]
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"] || package["repository"]["url"]
  s.license      = package["license"]
  s.author       = package["author"]
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/hieuvp/react-native-fingerprint-scanner.git", :tag => s.version }
  s.source_files  = "ios/*.{h,m}"
  s.requires_arc = true

  s.dependency "React"
end
