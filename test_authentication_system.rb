require 'active_support/all'
require 'addressable/uri'
require 'json'
require 'jwt'
require 'pry'
require 'sinatra'


priv_key = <<-KEY.strip_heredoc
KEY


### intialize #################################################################

def initialize
  super()
  @hooks = []
end


### Meta ######################################################################

get '/status' do
  'OK'
end



### sign-in ###################################################################

get '/sign-in' do
  token = params[:token]
  binding.pry
  'OK'
end


