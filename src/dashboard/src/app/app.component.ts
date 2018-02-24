import { Component } from '@angular/core';
import { HealthcheckService } from './healthcheck.service';
import { IService } from './shared/models/service.model';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  public services = [];
  public errorMessage: string = '';
  public infoMessage: string = '';
  hasError: boolean;
  hasInfo: boolean;

  constructor(private _healthcheckService: HealthcheckService) { }
  
  ngOnInit() {
    this._healthcheckService.getAllServices()
            .subscribe(data => {
              this.services = data;
            },
            error => {
              this.errorMessage = error;
              this.hasError = true;
              setTimeout(()=>{this.closeErrorMessage()}, 10000);
            });
  }
  
  onReloadClick() {
    this._healthcheckService.reloadServicesJson()
            .subscribe(data => {
              this.infoMessage = data;
              this.hasInfo = true;
              setTimeout(()=>{this.closeInfoMessage()}, 10000);
            },
            error => {
              this.errorMessage = error;
              this.hasError = true;
              setTimeout(()=>{this.closeErrorMessage()}, 10000);
            });
  }

  closeErrorMessage() {
    this.hasError = false;
  }

  closeInfoMessage() {
    this.hasInfo = false;
  }
}
