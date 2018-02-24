import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { IService } from './shared/models/service.model';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/catch';
import 'rxjs/add/observable/throw';
import { Options } from 'selenium-webdriver/firefox';
import { environment } from '../environments/environment';


@Injectable()
export class HealthcheckService {


  constructor(private http: HttpClient) { }

  getAllServices(): Observable<IService[]> {
    return this.http
                  .get<IService[]>(environment.healthcheckAllUrl)
                  .catch(this.errorHandler);
  }
  reloadServicesJson(){
    return this.http
                .get(environment.healthcheckReloadUrl, {responseType: 'text'})
                .catch(this.errorHandler);
  }
  errorHandler(error: HttpErrorResponse) {
    return Observable.throw(error.error.errorMessage || error.message);
  }
}
